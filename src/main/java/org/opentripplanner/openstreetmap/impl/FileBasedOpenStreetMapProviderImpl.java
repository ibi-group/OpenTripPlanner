package org.opentripplanner.openstreetmap.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;

public class FileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File _path;

    public void setPath(File path) {
        _path = path;
    }

    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            OpenStreetMapParser parser = new OpenStreetMapParser();
            if (_path.getName().endsWith(".gz")) {
                InputStream in = new GZIPInputStream(new FileInputStream(_path));
                parser.parseMap(in, handler);
            } else if (_path.getName().endsWith(".bz2")) {
                BZip2CompressorInputStream in = new BZip2CompressorInputStream(new FileInputStream(_path));
                parser.parseMap(in, handler);
            } else {
                parser.parseMap(_path, handler);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + _path, ex);
        }
    }

    public String toString() {
        return "FileBasedOpenStreetMapProviderImpl(" + _path + ")";
    }

    @Override
    public void checkInputs() {
        if (!_path.canRead()) {
            throw new RuntimeException("Can't read OSM path: " + _path);
        }
    }
}
