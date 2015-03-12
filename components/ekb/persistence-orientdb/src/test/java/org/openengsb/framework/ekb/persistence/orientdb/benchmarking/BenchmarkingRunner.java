package org.openengsb.framework.ekb.persistence.orientdb.benchmarking;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BenchmarkingRunner {

    Benchmarking benchmarking;

    @Before
    public void setUp()
    {
        benchmarking = new Benchmarking("F:\\orientdb-embedded",
            "C:\\Users\\sp\\Desktop\\benchmark-instances", "C:\\Users\\sp\\Desktop\\benchmark-results",
                100, 1000, 10000, 100000, 1000000);
    }


    @Test
    public void runAll() {
        benchmarking.runAll();
    }

    @Test
    public void run1Small() {
        benchmarking.createOrOverwriteResultFiles();
        benchmarking.runScenario(1, 10000);
    }

}
