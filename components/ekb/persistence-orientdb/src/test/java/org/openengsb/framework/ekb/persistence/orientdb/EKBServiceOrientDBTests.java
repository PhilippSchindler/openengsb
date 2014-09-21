package org.openengsb.framework.ekb.persistence.orientdb;

import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.core.api.model.OpenEngSBModelEntry;
import org.openengsb.framework.ekb.persistence.orientdb.models.*;
import org.openengsb.framework.ekb.persistence.orientdb.visualization.DotExporter;

import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

/**
 * Created by Philipp Schindler on 14.09.2014.
 */

@RunWith(MockitoJUnitRunner.class)
public class EKBServiceOrientDBTests {

    static Person[] persons;
    static Manager[] managers;
    static Activity[] activities;
    static Project[] projects;

    @BeforeClass
    public static void setUp() throws IOException {
        createDatabaseAndSchema();
        initializeTestData();
    }

    @AfterClass
    public static void cleanUp() {
        // drop database here
        // currently not doing this because of inspecting of the data with orientdb studio (orientdb gui)
    }

    @Test
    public void testAddInsert_shouldInsertModelIntoCommit() {
        EKBCommit commit = new EKBCommitImpl();
        commit.addOperation(new Operation(OperationType.INSERT, persons[0]));
    }

    @Test
    public void testCommit_shouldCreateDataAndVersioningInfos() {

        EKBServiceOrientDB service = new EKBServiceOrientDB();
        service.setDatabase(OrientDBHelper.getDefault().getConnection());

        EKBCommit c1 = new EKBCommitImpl();
        c1.addOperation(new Operation(OperationType.INSERT, persons[0]));
        c1.addOperation(new Operation(OperationType.INSERT, persons[1]));
        c1.addOperation(new Operation(OperationType.INSERT, persons[2]));
        c1.addOperation(new Operation(OperationType.INSERT, persons[3]));
        service.commit(c1);

        EKBCommit c2 = new EKBCommitImpl();
        c2.addOperation(new Operation(OperationType.INSERT, persons[4]));
        c2.addOperation(new Operation(OperationType.INSERT, persons[5]));
        service.commit(c2);

        EKBCommit c3 = new EKBCommitImpl();
        c3.addOperation(new Operation(OperationType.DELETE, persons[4]));
        service.commit(c3);

        EKBCommit c4 = new EKBCommitImpl();
        persons[0].setPassword("update01");
        c4.addOperation(new Operation(OperationType.UPDATE, persons[0]));
        service.commit(c4);

        EKBCommit c5 = new EKBCommitImpl();
        persons[0].setPassword("update02");
        c5.addOperation(new Operation(OperationType.UPDATE, persons[0]));
        c5.addOperation(new Operation(OperationType.INSERT, activities[0]));
        c5.addOperation(new Operation(OperationType.INSERT, activities[1]));
        c5.addOperation(new Operation(OperationType.INSERT, activities[2]));
        service.commit(c5);

        EKBCommit c6 = new EKBCommitImpl();
        Relationship r0 = new RelationshipImpl("performs", persons[0], activities[1]);
        c6.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, new RelationshipImpl("performs", persons[0], activities[0])));
        c6.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, r0));
        c6.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, new RelationshipImpl("performs", persons[0], activities[2])));
        service.commit(c6);

        EKBCommit c7 = new EKBCommitImpl();
        c7.addOperation(new Operation(OperationType.DELETE_RELATIONSHIP, r0));
        c7.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, new RelationshipImpl("performs", persons[1], activities[2])));
        service.commit(c7);

        EKBCommit c8 = new EKBCommitImpl();
        c8.addOperation(new Operation(OperationType.DELETE, persons[1]));
        service.commit(c8);

        DotExporter.export(service.getDatabase().getRawGraph(), "C:\\Users\\sp\\db.dot", "PersonHistory", "ActivityHistory");
    }

    @Test
    public void testComplexModel() {
        ComplexModel model = new ComplexModel();

        model.setSomeString("Hallo Welt");
        model.setSomeByte((byte) 47);
        model.setSomeShort((short) 4711);
        model.setSomeInteger(-1457777);
        model.setSomeLong(555555555555555L);
        model.setSomeFloat(47.11f);
        model.setSomeDouble(47.12);
        model.setSomeDecimal(new BigDecimal("123456789123456789123456789123456789123456789123456789123456789"));
        model.setSomeBoolean(true);
        model.setSomeBinary(new byte[]{1, 2, 4, 8, 16, 32, 64});

        Calendar c = Calendar.getInstance();
        c.set(2006, 05, 04, 03, 02, 01);
        model.setSomeDate(c.getTime());

        model.setSomeEmbeddedObject(new StringBuilder());

        List<Object> someEmbeddedList = new ArrayList<>();
        someEmbeddedList.add(new Person());
        someEmbeddedList.add(new Person());
        model.setSomeEmbeddedList(someEmbeddedList);

        Set<Object> someEmbeddedSet = new HashSet<>();
        someEmbeddedSet.add(12);
        someEmbeddedSet.add(15);
        someEmbeddedSet.add(20);
        model.setSomeEmbeddedSet(someEmbeddedSet);

        /*
            private String someString;
    private byte someByte;
    private short someShort;
    private int someInteger;
    private long someLong;
    private float someFloat;
    private double someDouble;
    private BigDecimal someDecimal;
    private boolean someBoolean;
    private Date someDate;
    private byte[] someBinary;

    // embedded types (Objects must be convertable to ODocument, e.g. the must be models?
    // embedded objects must not have a RID or uiid, the are only accessable via the outer model
    private Object someEmbeddedObject;
    private List<Object> someEmbeddedList;
    private Set<Object> someEmbeddedSet;
    private Map<String, Object> someEmbeddedMap;
         */

        EKBCommit commit = new EKBCommitImpl();
        commit.addOperation(new Operation(OperationType.INSERT, model));

        OpenEngSBModel openEngSBModel = commit.getOperations(OperationType.INSERT).get(0).getModel();
        for (OpenEngSBModelEntry entry : openEngSBModel.toOpenEngSBModelValues()) {

            System.out.println(entry.getKey() + ":   " + entry.getValue());

            if (entry.getType() == List.class) {
                for (Object o : (List<?>) entry.getValue()) {
                    OpenEngSBModel model2 = (OpenEngSBModel)o;
                    System.out.println("   " + model2);
                }
            }
        }
    }

    private static void createDatabaseAndSchema() throws IOException {
        OrientDBHelper.getDefault().createOrOverwriteDatabase();
        OrientGraphNoTx database = OrientDBHelper.getDefault().getConnectionNoTx();

        SchemaGenerator generator = new SchemaGenerator();
        generator.setDatabase(database);
        generator.generateVersioningSchema();

        generator.addModel(Activity.class);
        generator.addModel(Project.class);
        generator.addModel(Person.class);
        generator.addModel(Manager.class);  // manager must be added after person due to inheritance

        database.shutdown();
    }

    private static void initializeTestData() {
        persons = new Person[6];
        for (int i = 0; i < persons.length; i++ )
            persons[i] = new Person();

        persons[0].setFullname("Anna");
        persons[0].setPhoneNumbers(Arrays.asList("012/1000454", "+43555/8996333"));

        persons[1].setFullname("Bernd");
        persons[2].setFullname("Claus");
        persons[3].setFullname("Hans");
        persons[4].setFullname("Georg");
        persons[5].setFullname("Peter");


        activities = new Activity[3];
        for (int i = 0; i < activities.length; i++ )
            activities[i] = new Activity();

        activities[0].setDesciption("Activity 01");
        activities[1].setDesciption("Activity 02");
        activities[2].setDesciption("Activity 03");
    }



}
