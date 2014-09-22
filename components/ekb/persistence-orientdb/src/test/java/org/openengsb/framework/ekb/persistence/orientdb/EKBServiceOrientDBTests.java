package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.core.api.model.OpenEngSBModelEntry;
import org.openengsb.framework.ekb.persistence.orientdb.models.*;
import org.openengsb.framework.ekb.persistence.orientdb.visualization.DotExporter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by Philipp Schindler on 14.09.2014.
 */

@RunWith(MockitoJUnitRunner.class)
public class EKBServiceOrientDBTests {

    private static EKBServiceOrientDB service;

    static Person[] persons;
    static Activity[] activities;

    @Before
    public void setUp() throws IOException {
        createDatabaseAndSchema();
        service = new EKBServiceOrientDB();
        service.setDatabase(OrientDBHelper.getDefault().getConnection());
    }

    @After
    public void cleanUp() {
        // maybe drop database here (not doing because it's automatically recreated in setUp
        service.getDatabase().shutdown();
    }

    // old test
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

    private void export(String name, String... classes) {
        DotExporter.export(service.getDatabase().getRawGraph(), "C:\\Users\\sp\\Desktop\\dotexport\\" + name + ".dot", classes );
    }

    private List<ODocument> query(String orientSql) {
        return (List<ODocument>)service.nativeQuery(orientSql);
    }

    private Person createTestPerson(int i) {
        String[] names = new String[] { "Anna", "Bernd", "Claus", "Hans", "Tina" };
        String[] passwords = new String[] { "12345", "qwerty", "qwertz", "abc123", "password" };
        String[] uiids = new String[] { "akjha-askac", "1uiya-hask1", "1212a-45acw", "121ax-12789", "12nk1-mkdhn" };

        List<List<?>> phones = Arrays.asList(
            null,
            Arrays.asList(),
            Arrays.asList("01/23456789"),
            Arrays.asList("09/87654321", "+43/6641111111"),
            Arrays.asList("02/200000000", "080000011100", "7788112112")
        );

        Person person = new Person();
        person.setFullname(names[i]);
        person.setPassword(passwords[i]);
        person.setLogin(names[i].toLowerCase() + "@test.com");
        person.setPhoneNumbers((List<String>) phones.get(i));
        person.setUiid(uiids[i]);

        return person;
    }
    private Manager createTestManager() {
        Manager manager = new Manager();
        manager.setFullname("Gerold Wichtig");
        manager.setPassword("verygoodpassword");
        manager.setLogin("gerold@chef.com");
        manager.setPhoneNumbers(Arrays.asList("01/444444449", "0214442221"));
        manager.setUiid("12323-12345");
        manager.setSeniorManager(true);
        return manager;
    }
    private Activity createTestActivity(int i) {
        String[] desciptions = new String[] { "design engine part 16.1", "evualuate max trust", "contact supplier" };
        String[] uiids = new String[] { "mncn1-0askl", "hnnkm-1789a", "11212-00144" };
        int[] durations = new int[] { 45, 0, 2 };
        int[] expDurations = new int[] { 80, 40, 1 };
        boolean[] finished = new boolean[] { false, false, true };

        Activity activity = new Activity();
        activity.setDesciption(desciptions[i]);
        activity.setUiid(uiids[i]);
        activity.setDuration(durations[i]);
        activity.setExpectedDuration(expDurations[i]);
        activity.setFinished(finished[i]);

        return activity;
    }
    private EKBCommit createCommit() {
        return new EKBCommitImpl();
    }



    @Test
    public void testCommit_shouldInsert5PersonsAndAManager() {

        EKBCommit c1 = createCommit();
        for (int i = 0; i < 5; i++)
            c1.addOperation(new Operation(OperationType.INSERT, createTestPerson(i)));
        c1.addOperation(new Operation(OperationType.INSERT, createTestManager()));
        service.commit(c1);

        assertEquals(6, query("select from Person").size());
        assertEquals(1, query("select from Manager").size());
        assertEquals(6, query("select from PersonHistory").size());
        assertEquals(1, query("select from ManagerHistory").size());
        assertEquals(1, query("select from Commit").size());
        assertEquals(6, query("select from Revision").size());

        export("insert5PersonsAndAManager");
    }

    @Test
    public void testCommit_shouldPerformBasicUpdate() {

        Person p0 = createTestPerson(0);

        EKBCommit c1 = createCommit();
        c1.addOperation(new Operation(OperationType.INSERT, p0));
        service.commit(c1);

        p0.setPassword("newPassword");
        EKBCommit c2 = createCommit();
        c2.addOperation(new Operation(OperationType.UPDATE, p0));
        service.commit(c2);

        assertEquals(1, query("select from Person").size());
        assertEquals(1, query("select from PersonHistory").size());
        assertEquals(2, query("select from Commit").size());
        assertEquals(2, query("select from Revision").size());

        export("basicUpdate");
    }

    @Test
    public void testCommit_shouldPerformBasicDelete() {

        Person p0 = createTestPerson(0);

        EKBCommit c1 = createCommit();
        c1.addOperation(new Operation(OperationType.INSERT, p0));
        service.commit(c1);

        EKBCommit c2 = createCommit();
        c2.addOperation(new Operation(OperationType.DELETE, p0));
        service.commit(c2);

        assertEquals(0, query("select from Person").size());
        assertEquals(1, query("select from PersonHistory").size());
        assertEquals(2, query("select from Commit").size());
        assertEquals(1, query("select from Revision").size());

        export("basicDelete");
    }


}
