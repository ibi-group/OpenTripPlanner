package org.opentripplanner.graph_builder.module;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.csvreader.CsvReader;
import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.FareAttribute;
import org.onebusaway.gtfs.model.IdentityBean;
import org.onebusaway.gtfs.model.Pathway;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.opentripplanner.calendar.impl.MultiCalendarServiceImpl;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.gtfs.BikeAccess;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.edgetype.factory.PatternHopFactory;
import org.opentripplanner.routing.edgetype.factory.GtfsStopContext;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarSrvDataWithoutDatesForLocalizedSrvId;
import static org.opentripplanner.gtfs.mapping.GTFSToOtpTransitServiceMapper.mapGtfsDaoToOTPTransitService;

public class GtfsModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(GtfsModule.class);

    private EntityHandler counter = new EntityCounter();

    private FareServiceFactory fareServiceFactory;

    /** will be applied to all bundles which do not have the cacheDirectory property set */
    private File cacheDirectory;

    /** will be applied to all bundles which do not have the useCached property set */
    private Boolean useCached;

    Set<String> agencyIdsSeen = Sets.newHashSet();

    int nextAgencyId = 1; // used for generating agency IDs to resolve ID conflicts



    private List<GtfsBundle> gtfsBundles;

    /**
     * A Set of bundleFilenames that had an auto-generated Feed ID. This helps keep track of whether it is possible to
     * generate deterministic Feed IDs with this set of bundles.
     */
    private final Set<String> bundleFilenamesWithAutoGeneratedFeedId = new HashSet<>();

    /**
     * A Set to keep track of which gtfsFeedIds were auto-generated.
     */
    private final Set<String> autoGeneratedFeedIds = new HashSet<>();

    /**
     * A Set that keeps track of which agencies had auto-generated IDs. The values in here consist of the strings in the
     * template `{FeedID}{AgencyID}`.
     */
    private final Set<String> autoGeneratedAgencyIds = new HashSet<>();

    public Boolean getUseCached() {
        return useCached;
    }

    private Comparator<GtfsBundle> compareByFileName = Comparator.comparing(bundle -> bundle.getPath().getName());

    public GtfsModule(List<GtfsBundle> bundles) {
        List<GtfsBundle> defensiveCopy = new ArrayList<>(bundles);
        defensiveCopy.sort(compareByFileName);
        this.gtfsBundles = defensiveCopy;
    }

    public List<String> provides() {
        List<String> result = new ArrayList<String>();
        result.add("transit");
        return result;
    }

    public List<String> getPrerequisites() {
        return Collections.emptyList();
    }

    public void setFareServiceFactory(FareServiceFactory factory) {
        fareServiceFactory = factory;
    }

    @Override
    public void buildGraph(Graph graph, GraphBuilderModuleSummary graphBuilderModuleSummary) {
        // we're about to add another agency to the graph, so clear the cached timezone
        // in case it should change
        // OTP doesn't currently support multiple time zones in a single graph;
        // at least this way we catch the error and log it instead of silently ignoring
        // because the time zone from the first agency is cached
        graph.clearTimeZone();

        MultiCalendarServiceImpl calendarService = new MultiCalendarServiceImpl();
        GtfsStopContext stopContext = new GtfsStopContext();

        try {
            String fileNames = gtfsBundles.stream().map(b -> b.getPath().getName()).collect(Collectors.joining(", "));
            LOG.info("Processing GTFS files in the following order: {}", fileNames);
            for (GtfsBundle gtfsBundle : gtfsBundles) {
                GraphBuilderTaskSummary bundleTask = graphBuilderModuleSummary.addSubTask(
                    String.format("Process bundle: %s", gtfsBundle)
                );
                LOG.info(bundleTask.start());

                // apply global defaults to individual GTFSBundles (if globals have been set)
                if (cacheDirectory != null && gtfsBundle.cacheDirectory == null) {
                    gtfsBundle.cacheDirectory = cacheDirectory;
                }

                if (useCached != null && gtfsBundle.useCached == null) {
                    gtfsBundle.useCached = useCached;
                }

                OtpTransitService transitService = mapGtfsDaoToOTPTransitService(loadBundle(gtfsBundle));

                GtfsContext context = GtfsLibrary
                        .createContext(gtfsBundle.getFeedId(), transitService, calendarService);

                PatternHopFactory hf = new PatternHopFactory(context);

                hf.setStopContext(stopContext);
                hf.setFareServiceFactory(fareServiceFactory);
                hf.setMaxStopToShapeSnapDistance(gtfsBundle.getMaxStopToShapeSnapDistance());

                calendarService.addData(
                        createCalendarSrvDataWithoutDatesForLocalizedSrvId(transitService),
                        transitService
                );

                hf.subwayAccessTime = gtfsBundle.subwayAccessTime;
                hf.maxInterlineDistance = gtfsBundle.maxInterlineDistance;
                hf.run(graph);

                if (gtfsBundle.doesTransfersTxtDefineStationPaths()) {
                    hf.createTransfersTxtTransfers();
                }
                if (gtfsBundle.linkStopsToParentStations) {
                    hf.linkStopsToParentStations(graph);
                }
                if (gtfsBundle.parentStationTransfers) {
                    hf.createParentStationTransfers();
                }
                LOG.info(bundleTask.finish());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        GraphBuilderTaskSummary postBundleTask = graphBuilderModuleSummary.addSubTask("Post-bundle processing");
        LOG.info(postBundleTask.start());
        // We need to save the calendar service data so we can use it later
        graph.putService(
                org.opentripplanner.model.calendar.CalendarServiceData.class,
                calendarService.getData()
        );
        graph.updateTransitFeedValidity(calendarService.getData());

        graph.hasTransit = true;
        graph.calculateTransitCenter();

        // log information about the feed and agency IDs now present in the graph
        LOG.info("Loaded the following feeds and agencies into the graph:");
        for (String feedId : graph.getFeedIds()) {
            Collection<org.opentripplanner.model.Agency> feedAgencies = graph.getAgencies(feedId);
            if (feedAgencies.size() > 0) {
                LOG.info(
                    "Feed with ID `{}`{}",
                    feedId,
                    autoGeneratedFeedIds.contains(feedId)
                        ? " (ID was auto-generated)"
                        : ""
                );
                for (org.opentripplanner.model.Agency feedAgency : feedAgencies) {
                    LOG.info(
                        " - Agency `{}` with ID `{}`{}",
                        feedAgency.getName(),
                        feedAgency.getId(),
                        autoGeneratedAgencyIds.contains(feedId + feedAgency.getId())
                            ? " (ID was auto-generated)"
                            : ""
                    );
                }
            }
        }

        LOG.info(postBundleTask.finish());
    }

    /****
     * Private Methods
     ****/

    private GtfsMutableRelationalDao loadBundle(GtfsBundle gtfsBundle)
            throws IOException {

        StoreImpl store = new StoreImpl(new GtfsRelationalDaoImpl());
        store.open();
        LOG.info("reading {}", gtfsBundle.toString());

        GtfsFeedId gtfsFeedId = gtfsBundle.getFeedId();

        // check for duplicate bundle names and make sure that at most one of those bundles contains an auto-generated
        // feed ID. If there is more than one, non-deterministic Feed IDs could be generated.
        String bundleFilename = gtfsBundle.getPath().getName();
        if (bundleFilenamesWithAutoGeneratedFeedId.contains(bundleFilename)) {
            throw new RuntimeException(
                String.format(
                    "There are at least 2 GTFS bundles with the name `%s` that lack a feed_id value in a record in the feed_info.txt table. This will result in non-deterministic Feed ID generation!",
                    bundleFilename
                )
            );
        }
        if (gtfsFeedId.isAutoGenerated()) {
            bundleFilenamesWithAutoGeneratedFeedId.add(bundleFilename);
            autoGeneratedFeedIds.add(gtfsFeedId.getId());
        }

        // Read the agency table to determine if there is a single agency entry without an agency ID. This is used later
        // to determine whether an agency ID was auto-generated (it will be auto-generated to the bundle's feed ID if
        // the agency ID is missing in a single-agency feed). According to the GTFS spec, if there are multiple
        // agencies, then they must have an agency ID. Therefore, it is only necessary to read the first record.
        boolean agencyFileContainedOneAgencyWithoutId = false;
        InputStream agencyInputStream = gtfsBundle.getCsvInputSource().getResource("agency.txt");
        try {
            CsvReader result = new CsvReader(agencyInputStream, StandardCharsets.UTF_8);
            result.readHeaders();
            result.readRecord();
            String firstAgencyId = result.get("agency_id");
            if (firstAgencyId == null || firstAgencyId.trim().length() == 0) {
                agencyFileContainedOneAgencyWithoutId = true;
            }
        } finally {
            agencyInputStream.close();
        }

        GtfsReader reader = new GtfsReader();
        reader.setInputSource(gtfsBundle.getCsvInputSource());
        reader.setEntityStore(store);
        reader.setInternStrings(true);
        // Set the default Agency ID to be the Feed ID of this bundle.
        reader.setDefaultAgencyId(gtfsFeedId.getId());

        if (LOG.isDebugEnabled())
            reader.addEntityHandler(counter);

        if (gtfsBundle.getDefaultBikesAllowed())
            reader.addEntityHandler(new EntityBikeability(true));

        for (Class<?> entityClass : reader.getEntityClasses()) {
            LOG.info("reading entities: " + entityClass.getName());
            reader.readEntities(entityClass);
            store.flush();
            // NOTE that agencies are first in the list and read before all other entity types, so it is effective to
            // set the agencyId here. Each feed ("bundle") is loaded by a separate reader, so there is no risk of
            // agency mappings accumulating.
            if (entityClass == Agency.class) {
                for (Agency agency : reader.getAgencies()) {
                    String agencyId = agency.getId();
                    LOG.info("This Agency has the ID {}", agencyId);
                    boolean agencyIdIsAutogenerated = false;
                    // Make sure the combination of the FeedId and agencyId is unique
                    // Somehow, when the agency's id field is missing, OBA replaces it with the agency's name.
                    // TODO Figure out how and why this is happening.
                    if (agencyId == null || agencyIdsSeen.contains(gtfsFeedId.getId() + agencyId)) {
                        // Loop in case generated name is already in use.
                        String generatedAgencyId = null;
                        while (generatedAgencyId == null || agencyIdsSeen.contains(generatedAgencyId)) {
                            generatedAgencyId = "F" + nextAgencyId;
                            nextAgencyId++;
                        }
                        LOG.warn("The agency ID '{}' was already seen, or I think it's bad. Replacing with '{}'.", agencyId, generatedAgencyId);
                        reader.addAgencyIdMapping(agencyId, generatedAgencyId); // NULL key should work
                        agency.setId(generatedAgencyId);
                        agencyId = generatedAgencyId;
                        agencyIdIsAutogenerated = true;
                    } else {
                        // the agencyId was not null or was non-conflicting. This means that either the agencyId was
                        // derived from a record in the agency.txt table, or it was assigned the default AgencyId which
                        // is the feedID of this feed. Therefore, set whether the agencyID was auto-generated based on
                        // whether the agency.txt had a single agency record without an agency_id.
                        agencyIdIsAutogenerated = agencyFileContainedOneAgencyWithoutId;
                    }
                    String gtfsFeedIdAndAgencyId = gtfsFeedId.getId() + agencyId;
                    agencyIdsSeen.add(gtfsFeedIdAndAgencyId);
                    if (agencyIdIsAutogenerated) {
                        autoGeneratedAgencyIds.add(gtfsFeedIdAndAgencyId);
                    }
                }
            }
        }

        for (ShapePoint shapePoint : store.getAllEntitiesForType(ShapePoint.class)) {
            shapePoint.getShapeId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (Route route : store.getAllEntitiesForType(Route.class)) {
            route.getId().setAgencyId(reader.getDefaultAgencyId());
            generateRouteColor(route);
        }
        for (Stop stop : store.getAllEntitiesForType(Stop.class)) {
            stop.getId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (Trip trip : store.getAllEntitiesForType(Trip.class)) {
            trip.getId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (ServiceCalendar serviceCalendar : store.getAllEntitiesForType(ServiceCalendar.class)) {
            serviceCalendar.getServiceId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (ServiceCalendarDate serviceCalendarDate : store.getAllEntitiesForType(ServiceCalendarDate.class)) {
            serviceCalendarDate.getServiceId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (FareAttribute fareAttribute : store.getAllEntitiesForType(FareAttribute.class)) {
            fareAttribute.getId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (Pathway pathway : store.getAllEntitiesForType(Pathway.class)) {
            pathway.getId().setAgencyId(reader.getDefaultAgencyId());
        }

        store.close();
        return store.dao;
    }

    /**
     * Generates routeText colors for routes with routeColor and without routeTextColor
     *
     * If route doesn't have color or already has routeColor and routeTextColor nothing is done.
     *
     * textColor can be black or white. White for dark colors and black for light colors of routeColor.
     * If color is light or dark is calculated based on luminance formula:
     * sqrt( 0.299*Red^2 + 0.587*Green^2 + 0.114*Blue^2 )
     *
     * @param route
     */
    private void generateRouteColor(Route route) {
        String routeColor = route.getColor();
        //No route color - skipping
        if (routeColor == null) {
            return;
        }
        String textColor = route.getTextColor();
        //Route already has text color skipping
        if (textColor != null) {
            return;
        }

        Color routeColorColor = Color.decode("#"+routeColor);
        //gets float of RED, GREEN, BLUE in range 0...1
        float[] colorComponents = routeColorColor.getRGBColorComponents(null);
        //Calculates luminance based on https://stackoverflow.com/questions/596216/formula-to-determine-brightness-of-rgb-color
        double newRed = 0.299*Math.pow(colorComponents[0],2.0);
        double newGreen = 0.587*Math.pow(colorComponents[1],2.0);
        double newBlue = 0.114*Math.pow(colorComponents[2],2.0);
        double luminance = Math.sqrt(newRed+newGreen+newBlue);

        //For brighter colors use black text color and reverse for darker
        if (luminance > 0.5) {
            textColor = "000000";
        } else {
            textColor = "FFFFFF";
        }
        route.setTextColor(textColor);
    }

    private class StoreImpl implements GenericMutableDao {

        private GtfsMutableRelationalDao dao;

        StoreImpl(GtfsMutableRelationalDao dao) {
            this.dao = dao;
        }

        @Override
        public void open() {
            dao.open();
        }

        @Override
        public <T> T getEntityForId(Class<T> type, Serializable id) {
            return dao.getEntityForId(type, id);
        }

        @Override
        public void saveEntity(Object entity) {
            dao.saveEntity(entity);
        }

        @Override
        public void flush() {
            dao.flush();
        }

        @Override
        public void close() {
            dao.close();
        }

        @Override
        public <T> void clearAllEntitiesForType(Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <K extends Serializable, T extends IdentityBean<K>> void removeEntity(T entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Collection<T> getAllEntitiesForType(Class<T> type) {
            return dao.getAllEntitiesForType(type);
        }

        @Override
        public void saveOrUpdateEntity(Object entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateEntity(Object entity) {
            throw new UnsupportedOperationException();
        }
    }

    private static class EntityCounter implements EntityHandler {

        private Map<Class<?>, Integer> _count = new HashMap<Class<?>, Integer>();

        @Override
        public void handleEntity(Object bean) {
            int count = incrementCount(bean.getClass());
            if (count % 1000000 == 0)
                if (LOG.isDebugEnabled()) {
                    String name = bean.getClass().getName();
                    int index = name.lastIndexOf('.');
                    if (index != -1)
                        name = name.substring(index + 1);
                    LOG.debug("loading " + name + ": " + count);
                }
        }

        private int incrementCount(Class<?> entityType) {
            Integer value = _count.get(entityType);
            if (value == null)
                value = 0;
            value++;
            _count.put(entityType, value);
            return value;
        }

    }

    private static class EntityBikeability implements EntityHandler {

        private Boolean defaultBikesAllowed;

        public EntityBikeability(Boolean defaultBikesAllowed) {
            this.defaultBikesAllowed = defaultBikesAllowed;
        }

        @Override
        public void handleEntity(Object bean) {
            if (!(bean instanceof Trip)) {
                return;
            }

            Trip trip = (Trip) bean;
            if (defaultBikesAllowed && BikeAccess.fromTrip(trip) == BikeAccess.UNKNOWN) {
                BikeAccess.setForTrip(trip, BikeAccess.ALLOWED);
            }
        }
    }

    @Override
    public void checkInputs() {
        for (GtfsBundle bundle : gtfsBundles) {
            bundle.checkInputs();
        }
    }

    public List<GtfsBundle> getGtfsBundles() {
        return Collections.unmodifiableList(gtfsBundles);
    }
}
