package org.openengsb.framework.ekb.persistence.orientdb;

import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openengsb.framework.ekb.persistence.orientdb.models.Activity;
import org.openengsb.framework.ekb.persistence.orientdb.models.Manager;
import org.openengsb.framework.ekb.persistence.orientdb.models.Person;
import org.openengsb.framework.ekb.persistence.orientdb.models.Project;

import java.io.IOException;
import java.util.Arrays;

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
        service.commit(c7);
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
