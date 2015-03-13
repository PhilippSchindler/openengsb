package org.openengsb.framework.ekb.persistence.orientdb.benchmarking;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.commons.io.FileUtils;
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

    EKBServiceOrientDB _service;

    String _pathDatabase;
    String _pathTestdata;
    String _pathResults;
    int[] _scenarioSizes;
    boolean _useEmbeddedMode = true;
    boolean _createIndices = true;

    int _currentScenario;
    int _currentScenarioSize;
    int _currentCommit;

    String COMMIT_PERFORMANCE = "commit-performance.csv";
    String QUERY_PERFORMANCE  = "query-performance.csv";
    String RECORD_COUNTS      = "record-counts.csv";
    String DISK_USAGE         = "disk-usage.csv";
    String DISK_USAGE_DETAILS = "disk-usage-details.csv";
    String DATABASE_NAME      = "engineering-db";

    public static final String COMMIT_ID_PREFIX = "#11:"; // cluster id for commit vertices
    public static final String EPLAN_ID_PREFIX  = "#13:";
    public static final String VCDM_ID_PREFIX   = "#16:";
    public static final String OPM_ID_PREFIX    = "#19:";


    public Benchmarking(String pathDatabase, String pathTestdata, String pathResults, boolean useEmbeddedMode,
            boolean createIndices, int... scenarioSizes) {
        _pathDatabase = pathDatabase;
        _pathTestdata = pathTestdata;
        _pathResults = pathResults;
        _scenarioSizes = scenarioSizes;
        _useEmbeddedMode = useEmbeddedMode;
        _createIndices = createIndices;

        COMMIT_PERFORMANCE = Paths.get(_pathResults, COMMIT_PERFORMANCE).toString();
        QUERY_PERFORMANCE  = Paths.get(_pathResults, QUERY_PERFORMANCE).toString();
        RECORD_COUNTS      = Paths.get(_pathResults, RECORD_COUNTS).toString();
        DISK_USAGE         = Paths.get(_pathResults, DISK_USAGE).toString();
        DISK_USAGE_DETAILS = Paths.get(_pathResults, DISK_USAGE_DETAILS).toString();
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


            Runtime.getRuntime().gc();

            System.out.println("executing commit");
            long startTime = System.currentTimeMillis();

            _service.commit(commit);

            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;
            System.out.println("commit executed in " + delta/1000 + "s");
            append(COMMIT_PERFORMANCE, delta);


            long memory = Runtime.getRuntime().totalMemory();
            System.out.println(String.format("memory usage (heap): %d MB", memory/1024/1024));

            queries();
            recordCounts();
            diskUsage();

            System.out.println();
        }

        System.out.println("\n\n\n");
    }



    void queries() {
        System.out.println("running queries");

        long delta;
        delta = query01(); append(QUERY_PERFORMANCE, 1, delta);
        delta = query02(); append(QUERY_PERFORMANCE, 2, delta);
        delta = query03(); append(QUERY_PERFORMANCE, 3, delta);
        delta = query04(); append(QUERY_PERFORMANCE, 4, delta);
        delta = query05(); append(QUERY_PERFORMANCE, 5, delta);
        delta = query06(); append(QUERY_PERFORMANCE, 6, delta);
        delta = query07(); append(QUERY_PERFORMANCE, 7, delta);
    }

    // how many changes, deletions, and insertions have been made so far (over all commits)
    long query01() {

        String cmd =
            "select sum(inserts_eplan.size()), sum(updates_eplan.size()), sum(deletes_eplan.size()) " +
            "from Commit";

        long before = System.currentTimeMillis();

        query(cmd);

        long after = System.currentTimeMillis();
        return after - before;
    }

    // how many changes, deletions, and insertions have been made grouped by commit
    // => in real world example, I would use a day or a week basis instead of commits
    long query02() {

        String cmd =
            "select @rid, inserts_eplan.size(), updates_eplan.size(), deletes_eplan.size() from Commit";

        long before = System.currentTimeMillis();

        query(cmd);

        long after = System.currentTimeMillis();
        return after - before;
    }

    // how many changes, deletions, and insertions have been made in contrast to the previous commit
    // => i.e. what has changed since e.g., yesterday
    long query03() {

        String commitId = COMMIT_ID_PREFIX + _currentCommit;
        String cmd = String.format(
            "select inserts_eplan.size(), updates_eplan.size(), deletes_eplan.size() from %s",
            commitId);

        long before = System.currentTimeMillis();

        query(cmd);

        long after = System.currentTimeMillis();
        return after - before;
    }

    // number of elements grouped by component and commit                   ???
    long query04() {

        String cmd =
            "select @rid as commit, $i as insertsByComponent, $u as updatesByComponent, $d as deletesByComponent from Commit "
          + "let "
          + "    $i = (select value as comp, count(*) as inserts from (select expand(inserts_vcdm.last.kks3) from $parent.$current) group by value)), "
          + "    $u = (select value as comp, count(*) as updates from (select expand(updates_vcdm.kks3) from $parent.$current) group by value)), "
          + "    $d = (select value as comp, count(*) as deletes from (select expand(deletes_vcdm.last.kks3) from $parent.$current) group by value)) ";

        long before = System.currentTimeMillis();

        query(cmd);

        long after = System.currentTimeMillis();
        return after - before;
    }

    // 5.) retrieve all elements of "*.XQ05" after last commit              ???
    long query05() {

        String commitId = COMMIT_ID_PREFIX + _currentCommit;

        String cmd = String.format(
            "select expand($combined) "
          + "let $inserts = (select from ( select expand(inserts_vcdm.last) from %s) where kks3 = 'XQ05'), "
          + "    $updates = (select from ( select expand(inserts_vcdm)      from %s) where kks3 = 'XQ05'), "
          + "    $deletes = (select from ( select expand(inserts_vcdm.last) from %s) where kks3 = 'XQ05'), "
          + "    $combined = unionall( $inserts, $updates, $deletes )",
            commitId, commitId, commitId);

        long before = System.currentTimeMillis();

        query(cmd);

        long after = System.currentTimeMillis();
        return after - before;
    }

    // 6.) how often has a specific entry been updated => e.g., with signal number 13.ACA00.CE000.XQ67
    long query06() {

        // if we know there is a current version
        String cmd =
            "select eval('history.revisions.size() - 1') as changeCount from Eplan " +
            "where signal_number = '13.ACA00.CE000.XQ67'";

        // if we don't know if it has been deleted
        // String cmd =
        //    "select eval('revisions.size() - 1'), @rid as changeCount from EplanHistory " +
        //    "where last.signal_number = '13.ACA00.CE000.XQ67'";

        long before = System.currentTimeMillis();

        query(cmd);

        long after = System.currentTimeMillis();
        return after - before;
    }

    // 7.) how often has a component been updated (over all commits) => e.g., XQ05
    long query07() {

        // excluding count of updates for already deleted artifacts of XQ05
        String cmd =
            "select sum(eval('history.revisions.size() - 1')) as changeCount from Vcdm where kks3 = 'XQ05'";


        // including count of updates for already deleted artifacts of XQ05
        // String cmd =
        //       "select sum(eval('revisions.size() - 1')) as changeCount from VcdmHistory where last.kks3 = 'XQ05'";

        long before = System.currentTimeMillis();

        query(cmd);

        long after = System.currentTimeMillis();
        return after - before;
    }


    void recordCounts(String... classes) {
        System.out.println("gathering records statistics");
        for (String clazz : new String[] { "Commit", "Eplan", "Vcdm", "Opm",
                "EplanRevision", "VcdmRevision", "OpmRevision", "EplanHistory", "VcdmHistory", "OpmHistory"}) {
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

        if (_useEmbeddedMode) {
            try {
                // try to delete the database folder before create or overwrite to avoid restore if something failed
                FileUtils.deleteDirectory(Paths.get(_pathDatabase, DATABASE_NAME).toFile());
            } catch (IOException e) { }
            helper.setConnectionURL("plocal:" + _pathDatabase);
        }
        else
            helper.setConnectionURL("remote:localhost");

        try {
            helper.createOrOverwriteDatabase();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        ODatabaseDocument database = helper.getConnectionNoTx();

        createSchema(database);
        createIndices(database);

        database.close();

        // create service and connect to database
        _service = new EKBServiceOrientDB();
        _service.setDatabase(helper.getConnection());
        _service.setUIIDIndexSupportEnabled(false);
    }

    void createSchema(ODatabaseDocument database) {
        SchemaGenerator generator = new SchemaGenerator();
        generator.setDatabase(database);
        generator.generateVersioningSchema();
        generator.addModel(Eplan.class);
        generator.addModel(Vcdm.class);
        generator.addModel(Opm.class);
    }
    void createIndices(ODatabaseDocument database) {
        if (_createIndices) {
            OSchema schema = database.getMetadata().getSchema();
            schema.getClass("Eplan").createProperty("signal_number", OType.STRING)
                    .createIndex(OClass.INDEX_TYPE.UNIQUE);
            schema.getClass("Vcdm").createProperty("kks3", OType.STRING)
                    .createIndex(OClass.INDEX_TYPE.NOTUNIQUE);
            schema.getClass("VcdmRevision").createProperty("kks3", OType.STRING)
                    .createIndex(OClass.INDEX_TYPE.NOTUNIQUE);
        }
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
        List<ODocument> result = (List<ODocument>)_service.nativeQuery(orientSql);
        return result;
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
