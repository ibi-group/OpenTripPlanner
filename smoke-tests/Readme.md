## IBI Group OTP Smoke Tests

As of 2024 upstream OTP has over 4500 unit tests but is intentionally light on integration tests
as these tend to be brittle and hard to maintain over time. They also don't supply the desired
granularity to figure out a problem during development.

For this reason this folder contains scripts and configuration files for running an OTP instance for 
each OTP2 deployment that can then be used to run smoke tests against. 
These tests are intended as a last sanity check when we merge upstream changes because these tend to 
be very large. 
It reduces the burden on the reviewer and increases the confidence that the upstream merge won't 
break anything.

### Details

Each smoke test follows these steps:

1. Takes the latest upstream code
2. Downloads up-to-date GTFS and OSM files
3. Builds a complete graph
4. Starts an OTP instance
5. Possibly configures realtime updates for transit and vehicle rental
6. Sends API requests against OTP and asserts that the result is within expected parameters

Since the input data is variable, there is some maintenance involved because expected results can 
change over time. However, the upside is that catastrophic data errors are also caught early on.

### Example results

Here we see a screenshot of a typical smoke test run with the current locations (as of 2023) visible
in the left panel.

![Smoke tests screnshot](smoke-tests.png)

### Commands

If you want to prepare an OTP instance for the smoke tests, run the following commands to build the
jar, download all OSM and GTFS files and then build the graph:

```
# start in root OTP dir
cd OpenTripPlanner
mvn package -DskipTests
cd smoke-tests
make build-atlanta
```

After the graph is built you can then run OTP with:

```
make run-atlanta
```

And finally run the smoke tests with

```
# go back to the root directory
cd ..
mvn test -Djunit.tags.included="atlanta" -Djunit.tags.excluded=""
```

### CI

The CI workflow that does all of this automatically can be found
at [smoke-tests.yml](../.github/workflows/smoke-tests.yml).
