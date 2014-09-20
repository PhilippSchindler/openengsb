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
    public void testInsert_shouldCreateDataAndVersioningInfos() {
        EKBCommit commit = new EKBCommitImpl();
        commit.addOperation(new Operation(OperationType.INSERT, persons[0]));

        EKBServiceOrientDB service = new EKBServiceOrientDB();
        service.setDatabase(OrientDBHelper.getDefault().getConnection());

        service.commit(commit);
    }


    private static void createDatabaseAndSchema() throws IOException {
        OrientDBHelper.getDefault().createOrOverwriteDatabase();
        OrientGraphNoTx database = OrientDBHelper.getDefault().getConnectionNoTx();

        SchemaGenerator generator = new SchemaGenerator();
        generator.setDatabase(database);
        generator.addModel(Activity.class);
        generator.addModel(Project.class);
        generator.addModel(Person.class);
        generator.addModel(Manager.class);  // manager must be added after person due to inheritance

        generator.generateVersioningSchema();
        generator.generateSchemaForModels();

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
    }

}
