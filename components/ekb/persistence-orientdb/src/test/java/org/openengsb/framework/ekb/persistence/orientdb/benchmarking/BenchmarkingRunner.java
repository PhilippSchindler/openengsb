package org.openengsb.framework.ekb.persistence.orientdb.benchmarking;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BenchmarkingRunner {

    Benchmarking benchmarking;


    static final String DATABASE_PATH  = "F:\\orientdb-embedded";
    static final String TEST_DATA_PATH = "C:\\Users\\sp\\Desktop\\benchmark-instances";
    static final String RESULTS_PATH   = "C:\\Users\\sp\\Desktop\\benchmark-results";

//    static final int[] AVAILABLE_SCENARIO_SIZES = new int[] { 1000000 };
    static final int[] AVAILABLE_SCENARIO_SIZES = new int[] { 100, 1000, 10000, 100000 };

    static final boolean USE_EMBEDDED_MODE = true;
    static final boolean USE_INDICES       = true;



    @Before
    public void setUp()
    {
        benchmarking = new Benchmarking(DATABASE_PATH, TEST_DATA_PATH, RESULTS_PATH,
                USE_EMBEDDED_MODE, USE_INDICES, AVAILABLE_SCENARIO_SIZES);
    }


    @Test
    public void runAll() {
        // executes both scenarios for all scenario sizes from AVAILABLE_SCENARIO_SIZES
        // overwrites results files!
        benchmarking.runAll();
    }

//    @Test
//    public void runSingle() {
//        // runs a single scenario (scenario 1 of size 10000 in this case)
//        benchmarking.createOrOverwriteResultFiles();
//        benchmarking.runScenario(1, 10000);
//    }

}
