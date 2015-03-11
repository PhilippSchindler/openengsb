package org.openengsb.framework.ekb.persistence.orientdb.benchmarking;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.sun.javaws.exceptions.InvalidArgumentException;
import org.apache.commons.io.FileUtils;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openengsb.framework.ekb.persistence.orientdb.EKBCommit;
import org.openengsb.framework.ekb.persistence.orientdb.EKBServiceOrientDB;
import org.openengsb.framework.ekb.persistence.orientdb.OrientDBHelper;
import org.openengsb.framework.ekb.persistence.orientdb.SchemaGenerator;
import org.openengsb.framework.ekb.persistence.orientdb.benchmarking.models.Eplan;
import org.openengsb.framework.ekb.persistence.orientdb.benchmarking.models.Opm;
import org.openengsb.framework.ekb.persistence.orientdb.benchmarking.models.Vcdm;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;

public class Benchmarking {

    String _pathDatabase;
    String _pathTestdata;
    String _pathResults;
    int[] _scenarioSizes;

    int _currentScenario;
    int _currentScenarioSize;
    int _currentCommit;

    String COMMIT_PERFORMANCE = "commit-performance.csv";
    String QUERY_PERFORMANCE  = "query-performance.csv";
    String RECORD_COUNTS      = "record-counts.csv";
    String DISK_USAGE         = "disk-usage.csv";
    String DISK_USAGE_DETAILS = "disk-usage-details.csv";

    final String DATABASE_NAME = "engineering-db";
    final boolean EMBEDDED_MODE = false;

    EKBServiceOrientDB _service;

    String[] _queries = new String[]
    {

    };

    public Benchmarking(String[] cmdArgs) throws IllegalArgumentException {
        if (!parseArgs(cmdArgs)) {
            printUsage();
            throw new IllegalArgumentException();
        }

        COMMIT_PERFORMANCE = Paths.get(_pathResults, COMMIT_PERFORMANCE).toString();
        QUERY_PERFORMANCE  = Paths.get(_pathResults, QUERY_PERFORMANCE).toString();
        RECORD_COUNTS      = Paths.get(_pathResults, RECORD_COUNTS).toString();
        DISK_USAGE         = Paths.get(_pathResults, DISK_USAGE).toString();
        DISK_USAGE_DETAILS = Paths.get(_pathResults, DISK_USAGE_DETAILS).toString();
    }

    public Benchmarking(String pathDatabase, String pathTestdata, String pathResults, int... scenarioSizes) {
        _pathDatabase = pathDatabase;
        _pathTestdata = pathTestdata;
        _pathResults = pathResults;
        _scenarioSizes = scenarioSizes;

        COMMIT_PERFORMANCE = Paths.get(_pathResults, COMMIT_PERFORMANCE).toString();
        QUERY_PERFORMANCE  = Paths.get(_pathResults, QUERY_PERFORMANCE).toString();
        RECORD_COUNTS      = Paths.get(_pathResults, RECORD_COUNTS).toString();
        DISK_USAGE         = Paths.get(_pathResults, DISK_USAGE).toString();
        DISK_USAGE_DETAILS = Paths.get(_pathResults, DISK_USAGE_DETAILS).toString();
    }

    public static void main(String[] args) {

        Benchmarking benchmarking = null;
        try {
            benchmarking = new Benchmarking(args);
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        if (benchmarking != null)
            benchmarking.runAll();
    }

    public void runAll() {
        createOrOverwriteResultFiles();

        for (int i = 0; i < _scenarioSizes.length; i++) {
            _currentScenarioSize = _scenarioSizes[i];
            for (_currentScenario = 1; _currentScenario <= 2; _currentScenario++) {
                resetDatabase();
                executeScenario();
                closeDatabase();
            }
        }
    }

    public void runScenario(int scenario, int scenarioSize) {
        resetDatabase();
        _currentScenario = scenario;
        _currentScenarioSize = scenarioSize;
        executeScenario();
        closeDatabase();
    }

    void executeScenario()
    {
        System.out.println("===================================================================");
        System.out.println(String.format("RUNNING BENCHMARKS FOR SCENARIO %d, SIZE %d",
                _currentScenario, _currentScenarioSize));
        System.out.println("===================================================================\n\n");


        int numCommits =  _currentScenario == 1 ? 21 : 14;
        for (_currentCommit = 0; _currentCommit < numCommits; _currentCommit++) {

            System.out.println("----- COMMIT " + _currentCommit + " -----");

            System.out.println("loading commmit data from file");
            EKBCommit commit = ScenarioLoader.loadCommit(
                    Paths.get(_pathTestdata, String.valueOf(_currentScenarioSize)).toString(),
                    _currentScenario, _currentCommit);
            if (commit == null) return;

            System.out.println("executing commit");
            long startTime = System.currentTimeMillis();

            _service.commit(commit);

            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;
            System.out.println("commit executed in " + delta/1000 + "s");
            append(COMMIT_PERFORMANCE, delta);

            queries();
            recordCounts();
            diskUsage();

            System.out.println();
        }

        System.out.println("\n\n\n");
    }


    void printUsage() {
        System.out.println("Usage:");
        System.out.println("   benchmarking <path-for-database> <path-for-testdata> <path-for-results> <[ instance sizes ]>");
        System.out.println("Sample: ");
        System.out.println("   benchmarking C:\\OrientDB C:\\instances C:\\results 5000 10000 50000 1000000");
    }

    boolean parseArgs(String[] args) {

        if (args.length < 3) {
            System.out.println("Arguments missing!");
            return false;
        }

        _pathDatabase = args[0];
        _pathTestdata = args[1];
        _pathResults  = args[2];

        _scenarioSizes = new int[args.length - 3];
        for (int i = 0; i < _scenarioSizes.length; i++) {
            _scenarioSizes[i] = Integer.parseInt(args[3 + i]);
        }


        if (!new File(_pathDatabase).exists()) {
            System.out.println("No valid directory for database found!");
            return false;
        }
        if (!new File(_pathTestdata).exists()) {
            System.out.println("No valid directory for test instances found!");
            return false;
        }
        for (int s : _scenarioSizes) {
            if (!Paths.get(_pathTestdata, String.valueOf(s)).toFile().exists()) {
                System.out.println("Test instances missing - use genernate_all.py for creating!");
                return false;
            }
        }

        if (!new File(_pathResults).exists()) {
            System.out.println("No valid directory for results found!");
            return false;
        }

        return true;
    }


    void queries() {
        System.out.println("running queries");
        for (int i = 0; i < _queries.length; i++ ) {
            long before = System.currentTimeMillis();
            query(_queries[i]);
            long after = System.currentTimeMillis();
            long delta = after - before;
            append(QUERY_PERFORMANCE, i, delta);
        }
    }

    void recordCounts(String... classes) {
        System.out.println("gathering records statistics");
        for (String clazz : new String[] { "Commit", "Revision", "Eplan", "Vcdm", "Opm",
                                           "EplanHistory", "VcdmHistory", "OpmHistory"}) {
            String recordCount = querySingle("select count(*) from " + clazz).toString();
            append(RECORD_COUNTS, clazz, recordCount);
        }
    }

    void diskUsage() {
        System.out.println("gathering disk usage statistics");
        long totalDBSize = 0;
        for (File file : Paths.get(_pathDatabase, DATABASE_NAME).toFile().listFiles()) {
            append(DISK_USAGE_DETAILS, file.getName(), file.length());
            totalDBSize += file.length();
        }
        append(DISK_USAGE, totalDBSize);
    }

    void resetDatabase() {

        System.out.println("reseting database, establishing connection");

        // create database
        OrientDBHelper helper = new OrientDBHelper();
        helper.setDatabaseName(DATABASE_NAME);
        helper.setUser("admin");
        helper.setPassword("admin");
        helper.setStorageType("plocal");

        if (EMBEDDED_MODE)
            helper.setConnectionURL("plocal:" + _pathDatabase);
        else
            helper.setConnectionURL("remote:localhost");

        try {
            helper.createOrOverwriteDatabase();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // create schema
        ODatabaseDocument database = helper.getConnectionNoTx();
        SchemaGenerator generator = new SchemaGenerator();
        generator.setDatabase(database);
        generator.generateVersioningSchema();
        generator.addModel(Eplan.class);
        generator.addModel(Vcdm.class);
        generator.addModel(Opm.class);
        database.close();

        // create service and connect to database
        _service = new EKBServiceOrientDB();
        _service.setDatabase(helper.getConnection());
        _service.setUIIDIndexSupportEnabled(false);
    }

    void closeDatabase() {
        if (_service != null) {
            _service.getDatabase().close();
            _service = null;
        }
    }

    public void createOrOverwriteResultFiles()
    {
        new File(COMMIT_PERFORMANCE).delete();
        new File(QUERY_PERFORMANCE).delete();
        new File(RECORD_COUNTS).delete();
        new File(DISK_USAGE).delete();
        new File(DISK_USAGE_DETAILS).delete();

        appendHeaders();
    }



    List<ODocument> query(String orientSql) {
        return (List<ODocument>) _service.nativeQuery(orientSql);
    }

    Object querySingle(String orientSql) {
        List<ODocument> docs = (List<ODocument>) _service.nativeQuery(orientSql);
        ODocument result = docs.get(0);
        return result.fieldValues()[0];     // TODO CHECK THIS
    }

    void appendPlain(String where, Object what) {
        try {
            FileUtils.writeStringToFile(new File(where), what.toString()+"\r\n", true);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    void append(String where, Object... what)
    {
        String line = _currentScenario + ";" + _currentScenarioSize + ";" + _currentCommit;
        for (Object w : what)
            line += ";" + w.toString();

        appendPlain(where, line);
    }

    void appendHeaders() {
        appendPlain(COMMIT_PERFORMANCE, "scenario;scenario-size;commit;time-in-ms");
        appendPlain(QUERY_PERFORMANCE, "scenario;scenario-size;commit;query;time-in-ms");
        appendPlain(RECORD_COUNTS, "scenario;scenario-size;commit;record-type;number-of-records");
        appendPlain(DISK_USAGE, "scenario;scenario-size;commit;total-disk-usage");
        appendPlain(DISK_USAGE_DETAILS, "scenario;scenario-size;commit;file;file-size");
    }
}
