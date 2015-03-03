package org.openengsb.framework.ekb.persistence.orientdb.benchmarking;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openengsb.core.ekb.api.*;
import org.openengsb.framework.ekb.persistence.orientdb.*;
import org.openengsb.framework.ekb.persistence.orientdb.EKBCommit;
import org.openengsb.framework.ekb.persistence.orientdb.visualization.DotExporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class BenchmarkTests {

    private static EKBServiceOrientDB service;
    public static final boolean DO_GRAPHICAL_EXPORT = false;

    @Before
    public void setUp() throws IOException {
        createDatabaseAndSchema();
        service = new EKBServiceOrientDB();
        service.setDatabase(OrientDBHelper.getDefault().getConnection());
        service.setUIIDIndexSupportEnabled(false);
    }

    @After
    public void cleanUp() {
        // maybe drop database here (not doing this now because it's automatically recreated in setUp
        // and it's useful to test queries on resulting database
        service.getDatabase().close();
    }

    private static void createDatabaseAndSchema() throws IOException {
        OrientDBHelper.getDefault().createOrOverwriteDatabase();
        ODatabaseDocument database = OrientDBHelper.getDefault().getConnectionNoTx();

        SchemaGenerator generator = new SchemaGenerator();
        generator.setDatabase(database);
        generator.generateVersioningSchema();
        generator.addModel(Signal.class);

        database.close();
    }

    private void export(String name, String... classes) {
        if (DO_GRAPHICAL_EXPORT) {
            DotExporter.export(service.getDatabase(), "C:\\Users\\sp\\Desktop\\dotexport\\" + name + ".dot", classes);
        }
    }

    private List<ODocument> query(String orientSql) {
        return (List<ODocument>) service.nativeQuery(orientSql);
    }


//    @Test
//    public void testScenarioLoader_shouldLoadCsvFile() throws IOException
//    {
//        EKBCommit commit = ScenarioLoader.loadCommit("C:\\Users\\sp\\Desktop\\benchmark-instances\\100", 1, 0);
//    }

//    @Test
//    public void testScenarioLoader_testCommit() throws IOException
//    {
//        EKBCommit commit = ScenarioLoader.loadCommit("C:\\Users\\sp\\Desktop\\benchmark-instances\\100", 1, 0);
//        service.commit(commit);
//    }

    @Test
    public void testScenarioLoader_testScenario() throws IOException
    {
        executeScenario(1, 1000000);
    }


    final String BENCHMARK_INSTANCES_PATH = "C:\\Users\\sp\\Desktop\\benchmark-instances";
    public void executeScenario(int scenario, int size) throws IOException
    {
        String baseFolder = Paths.get(BENCHMARK_INSTANCES_PATH, String.valueOf(size)).toString();
        int numCommits = Paths.get(baseFolder, "scenario_" + scenario).toFile().listFiles().length / 3;

        for (int commitNr = 0; commitNr < numCommits; commitNr++)
        {
            EKBCommit commit = ScenarioLoader.loadCommit(baseFolder, scenario, commitNr);


            long startTime = System.currentTimeMillis();


            service.commit(commit);

            long endTime = System.currentTimeMillis();

            System.out.println("commit " + commitNr + " executed in " + (endTime - startTime)/1000 + "s");

            // execute queries and gather query performance measurements TODO
        }
    }



//    @Test
//    public void testCommit_shouldInsert5PersonsAndAManager() {
//
//        EKBCommit c1 = createCommit();
//        for (int i = 0; i < 5; i++)
//            c1.addOperation(new Operation(OperationType.INSERT, createTestPerson(i)));
//        c1.addOperation(new Operation(OperationType.INSERT, createTestManager()));
//        service.commit(c1);
//
//        assertEquals(6, query("select from Person").size());
//        assertEquals(1, query("select from Manager").size());
//        assertEquals(6, query("select from PersonHistory").size());
//        assertEquals(1, query("select from ManagerHistory").size());
//        assertEquals(1, query("select from Commit").size());
//        assertEquals(6, query("select from Revision").size());
//
//        export("insert5PersonsAndAManager");
//    }


}
