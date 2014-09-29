/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.impl.local.paginated.base.ODurableComponent;
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
import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class EKBServiceOrientDBTests {

    private static EKBServiceOrientDB service;
    public static final boolean DO_GRAPHICAL_EXPORT = true;

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
        service.getDatabase().close();
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

        DotExporter.export(service.getDatabase(), "C:\\Users\\sp\\db.dot", "PersonHistory", "ActivityHistory");
    }

    private static void createDatabaseAndSchema() throws IOException {
        OrientDBHelper.getDefault().createOrOverwriteDatabase();
        ODatabaseDocument database = OrientDBHelper.getDefault().getConnectionNoTx();

        SchemaGenerator generator = new SchemaGenerator();
        generator.setDatabase(database);
        generator.generateVersioningSchema();

        generator.addModel(Activity.class);
        generator.addModel(Project.class);
        generator.addModel(Person.class);
        generator.addModel(Manager.class);  // manager must be added after person due to inheritance

        database.close();
    }

    private void export(String name, String... classes) {
        if (DO_GRAPHICAL_EXPORT)
            DotExporter.export(service.getDatabase(),
                    "C:\\Users\\sp\\Desktop\\dotexport\\" + name + ".dot", classes );
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
    private Relationship createRelationship(String name, Object... relatedModels) {
        return new RelationshipImpl(name, relatedModels);
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

    @Test
    public void testCommit_shouldInsertRelationship() {

        Person p0 = createTestPerson(0);
        Activity a0 = createTestActivity(0);
        Relationship r0 = createRelationship("performs", p0, a0);

        EKBCommit c1 = createCommit();
        c1.addOperation(new Operation(OperationType.INSERT, p0));
        c1.addOperation(new Operation(OperationType.INSERT, a0));
        c1.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, r0));
        service.commit(c1);

        // one for the revision and one for the current
        assertEquals(2, query("select from Relationship").size());

        export("insertRelationship");
    }

    @Test
    public void testCommit_shouldDropRelationship() {

        Person p0 = createTestPerson(0);
        Activity a0 = createTestActivity(0);
        Relationship r0 = createRelationship("performs", p0, a0);

        EKBCommit c1 = createCommit();
        c1.addOperation(new Operation(OperationType.INSERT, p0));
        c1.addOperation(new Operation(OperationType.INSERT, a0));
        c1.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, r0));
        service.commit(c1);

        EKBCommit c2 = createCommit();
        c2.addOperation(new Operation(OperationType.DELETE_RELATIONSHIP, r0));
        service.commit(c2);

        // one for the revision
        assertEquals(1, query("select from Relationship").size());

        export("deleteRelationship");
    }

    @Test
    public void testCommit_relationshipShouldBehaveCorrectOnUpdateOfRelatedModels() {

        Person p0 = createTestPerson(0);
        Activity a0 = createTestActivity(0);
        Relationship r0 = createRelationship("performs", p0, a0);

        // 1st commit - no relationship here
        EKBCommit c1 = createCommit();
        c1.addOperation(new Operation(OperationType.INSERT, p0));
        c1.addOperation(new Operation(OperationType.INSERT, a0));
        service.commit(c1);

        // 2nd commit - both related nodes are updated
        // nodes should be related now
        p0.setPassword("newPassword1");
        a0.setExpectedDuration(a0.getExpectedDuration() + 10);
        EKBCommit c2 = createCommit();
        c2.addOperation(new Operation(OperationType.UPDATE, p0));
        c2.addOperation(new Operation(OperationType.UPDATE, a0));
        c2.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, r0));
        service.commit(c2);

        // 3rd commit - activity is updated again
        // nodes should still be related
        a0.setExpectedDuration(a0.getExpectedDuration() + 10);
        EKBCommit c3 = createCommit();
        c3.addOperation(new Operation(OperationType.UPDATE, a0));
        service.commit(c3);

        // one for the revisions and one for the current version
        assertEquals(2, query("select from Relationship").size());

        export("complexRelationshipInsertBehaviour");
    }

    @Test
    public void testCommit_relationshipShouldBehaveCorrectOnUpdateOfRelatedModelsAndDelete() {
        // like the previous testcase
        // additionally deletes the relationship in the end and updates the activity again
        // so the last activity revision should not be related to the person

        Person p0 = createTestPerson(0);
        Activity a0 = createTestActivity(0);
        Relationship r0 = createRelationship("performs", p0, a0);

        // 1st commit - no relationship here
        EKBCommit c1 = createCommit();
        c1.addOperation(new Operation(OperationType.INSERT, p0));
        c1.addOperation(new Operation(OperationType.INSERT, a0));
        service.commit(c1);

        // 2nd commit - both related nodes are updated
        // nodes should be related now
        p0.setPassword("newPassword1");
        a0.setExpectedDuration(a0.getExpectedDuration() + 10);
        EKBCommit c2 = createCommit();
        c2.addOperation(new Operation(OperationType.UPDATE, p0));
        c2.addOperation(new Operation(OperationType.UPDATE, a0));
        c2.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, r0));
        service.commit(c2);

        // 3rd commit - activity is updated again
        // nodes should still be related
        a0.setExpectedDuration(a0.getExpectedDuration() + 10);
        EKBCommit c3 = createCommit();
        c3.addOperation(new Operation(OperationType.UPDATE, a0));
        service.commit(c3);

        // end of previous testcase here

        a0.setExpectedDuration(a0.getExpectedDuration() + 10);
        EKBCommit c4 = createCommit();
        c4.addOperation(new Operation(OperationType.UPDATE, a0));
        c4.addOperation(new Operation(OperationType.DELETE_RELATIONSHIP, r0));
        service.commit(c4);

        // one for the revisions
        assertEquals(1, query("select from Relationship").size());

        export("complexRelationshipInsertBehaviourWithDelete");
    }

    @Test
    public void testCommit_relationshipShouldBeDeletedAfterRelatedModelIsDeleted() {
        // if we delete a model all relationships with this model must be archived as well
        // however the deletion is not cascaded - so related model are not deleted

        Person p0 = createTestPerson(0);
        Activity a0 = createTestActivity(0);
        Relationship r0 = createRelationship("performs", p0, a0);

        EKBCommit c1 = createCommit();
        c1.addOperation(new Operation(OperationType.INSERT, p0));
        c1.addOperation(new Operation(OperationType.INSERT, a0));
        c1.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, r0));
        service.commit(c1);

        EKBCommit c2 = createCommit();
        c2.addOperation(new Operation(OperationType.DELETE, a0));
        service.commit(c2);

        // one for the revisions
        assertEquals(1, query("select from Relationship").size());

        export("relationshipBehavourOnDeleleOfRelatedModel");
    }


    @Test
         public void testCommit_multipleRelationshipsShouldBeCreated() {

        Person p0 = createTestPerson(0);
        Activity a0 = createTestActivity(0);
        Activity a1 = createTestActivity(1);

        Relationship r0 = createRelationship("performs", p0, a0);
        Relationship r1 = createRelationship("performs", p0, a1);

        EKBCommit c1 = createCommit();
        c1.addOperation(new Operation(OperationType.INSERT, p0));
        c1.addOperation(new Operation(OperationType.INSERT, a0));
        c1.addOperation(new Operation(OperationType.INSERT, a1));
        c1.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, r0));
        c1.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, r1));
        service.commit(c1);

        // two for the revisions, and two for the current versions of the relationship
        assertEquals(4, query("select from Relationship").size());

        export("insertMultipleRelationships");
    }

    @Test
    public void testCommit_multipleRelationshipsDelete() {

        Person p0 = createTestPerson(0);
        Activity a0 = createTestActivity(0);
        Activity a1 = createTestActivity(1);

        Relationship r0 = createRelationship("performs", p0, a0);
        Relationship r1 = createRelationship("performs", p0, a1);

        EKBCommit c1 = createCommit();
        c1.addOperation(new Operation(OperationType.INSERT, p0));
        c1.addOperation(new Operation(OperationType.INSERT, a0));
        c1.addOperation(new Operation(OperationType.INSERT, a1));
        c1.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, r0));
        c1.addOperation(new Operation(OperationType.INSERT_RELATIONSHIP, r1));
        service.commit(c1);

        EKBCommit c2 = createCommit();
        c2.addOperation(new Operation(OperationType.DELETE_RELATIONSHIP, r1));
        service.commit(c2);

        // two for the revisions, and two for the current versions of the relationship
        assertEquals(3, query("select from Relationship").size());

        export("deleteMultipleRelationships");
    }

    // TODO negativ tests for illegal commits
    // e.g. insert of relationship where model are not in the db or are deleted in same commit...


    @Test
    public void testComplexModel() {

        ComplexModel model = prepareComplexModel();

//        EKBCommit commit = new EKBCommitImpl();
//        commit.addOperation(new Operation(OperationType.INSERT, model));

        assert(OType.isSimpleType(model.getSomeString()));
        assert(OType.isSimpleType(model.getSomeByte()));
        assert(OType.isSimpleType(model.getSomeShort()));
        assert(OType.isSimpleType(model.getSomeInteger()));
        assert(OType.isSimpleType(model.getSomeLong()));
        assert(OType.isSimpleType(model.getSomeFloat()));
        assert(OType.isSimpleType(model.getSomeDouble()));
        assert(OType.isSimpleType(model.getSomeDecimal()));
        assert(OType.isSimpleType(model.getSomeBoolean()));

        assertFalse(OType.isSimpleType(model.getSomeEmbeddedObject()));
        assertFalse(OType.isSimpleType(model.getSomeEmbeddedList()));
        assertFalse(OType.isSimpleType(model.getSomeEmbeddedSet()));
        assertFalse(OType.isSimpleType(model.getSomeEmbeddedMap()));


        ODocument document = service.getDatabase().newInstance("V");
        service.convertModel((OpenEngSBModel)model, document);

        document.save();
        service.getDatabase().commit();

        assertEquals(1, query("select from V").size());
    }

    private ComplexModel prepareComplexModel() {
        ComplexModel model = new ComplexModel();

        model.setRID("#12:1");
        model.setUiid("xaaza-ashka");

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

        model.setSomeEmbeddedObject(createTestActivity(0));

        List<Object> someEmbeddedList = new ArrayList<>();
        someEmbeddedList.add(createTestPerson(0));
        someEmbeddedList.add(createTestPerson(1));
        someEmbeddedList.add(createTestPerson(2));
        someEmbeddedList.add(createTestPerson(3));
        someEmbeddedList.add(createTestPerson(4));
        model.setSomeEmbeddedList(someEmbeddedList);

        Set<Object> someEmbeddedSet = new HashSet<>();
        someEmbeddedSet.add(12);
        someEmbeddedSet.add("keine zahl");
        someEmbeddedSet.add(20);
        model.setSomeEmbeddedSet(someEmbeddedSet);

        Map<String, Object> someEmbeddedMap = new HashMap<>();
        someEmbeddedMap.put("city", "Vienna");
        someEmbeddedMap.put("people", 1500000);
        someEmbeddedMap.put("someactivity", createTestActivity(1));
        model.setSomeEmbeddedMap(someEmbeddedMap);

        return model;
    }

    @Test
    public void testIndex_shouldAddAndRetrievePersonViaIndex() {
        Person p0 = createTestPerson(0);

        EKBCommit c1 = createCommit();
        c1.addOperation(new Operation(OperationType.INSERT, p0));
        service.commit(c1);

        OIndex<?> uiid = service.getDatabase().getMetadata().getIndexManager().getIndex("uiid");
        uiid.put(p0.getUiid(), new ORecordId(p0.getRID()));
        service.getDatabase().commit();

        assertEquals(1, query("select from index:uiid where key = \"" + p0.getUiid() + "\"").size());
    }

    @Test
    public void testIndex_updateIndex() {
        Person p0 = createTestPerson(0);
        Person p1 = createTestPerson(1);

        EKBCommit c1 = createCommit();
        c1.addOperation(new Operation(OperationType.INSERT, p0));
        service.commit(c1);

        OIndex<?> uiid = service.getDatabase().getMetadata().getIndexManager().getIndex("uiid");
        uiid.put(p0.getUiid(), new ORecordId(p0.getRID()));
        service.getDatabase().commit();

        EKBCommit c2 = createCommit();
        c2.addOperation(new Operation(OperationType.INSERT, p1));
        service.commit(c2);

        uiid.remove(p0.getUiid());
        uiid.put(p0.getUiid(), new ORecordId(p1.getRID()));
        service.getDatabase().commit();

        ODocument doc_p1 = query("select rid.fullname as 'fullname' from index:uiid where key = \""
                + p0.getUiid() + "\"").get(0);
        assertEquals(p1.getFullname(), doc_p1.field("fullname"));
    }

    @Test
    public void testCommitPerformance_add100000Persons() {
        EKBCommit c1 = createCommit();
        for (int i = 0; i < 100000; i++) {
            Person p =  createTestPerson(0);
            p.setUiid(UUID.randomUUID().toString());
            c1.addOperation(new Operation(OperationType.INSERT, p));
        }

        long start = System.currentTimeMillis();
        service.commit(c1);
        long stop = System.currentTimeMillis();

        long time = stop - start;

        System.out.println("inserted 100000 persons in: " + time +  "ms");
    }

    @Test
    public void testCommitPerformance_add100000Documents() {

        long start = System.currentTimeMillis();

        ODatabaseDocumentTx database = service.getDatabase();

        ODocument current;
        ODocument revision;
        ODocument history;
        ODocument commit = database.newInstance("Commit");

        current = database.newInstance("Person");
        revision = database.newInstance("Revision");
        history = database.newInstance("PersonHistory");

        List<ORID> inserts = new ArrayList<>();

        for (int i = 0; i < 100000; i++) {
            Person p =  createTestPerson(0);
            p.setUiid(UUID.randomUUID().toString());

            current.reset();
            revision.reset();
            history.reset();

//            current = database.newInstance("Person");
//            revision = database.newInstance("Revision");
//            history = database.newInstance("PersonHistory");

            current.field("uiid", p.getUiid());
            current.field("fullname", p.getFullname());
            current.field("login", p.getLogin());
            current.field("password", p.getPassword());
            current.field("phoneNumbers", p.getPhoneNumbers());
            current.field("history", history);

            revision.field("uiid", p.getUiid());
            revision.field("fullname", p.getFullname());
            revision.field("login", p.getLogin());
            revision.field("password", p.getPassword());
            revision.field("phoneNumbers", p.getPhoneNumbers());
            revision.field("history", history);

            history.field("createdBy", commit);
            history.field("deleteBy", (ODocument) null);
            history.field("archived", false);
            history.field("current", current);
            history.field("last",  revision);
            history.field("first", revision);
            List<ODocument> linkRevisions = new ArrayList<>();
            linkRevisions.add(revision);
            history.field("revisions", linkRevisions);

            current.save(); // saves history and revision as well
            inserts.add(history.getIdentity().copy());
        }

        commit.field("inserts", inserts);
        commit.save();

        database.commit();


        long stop = System.currentTimeMillis();
        long time = stop - start;

        System.out.println("without overhead inserted 300000 vertices in: " + time +  "ms");

        // approx 4x speedup, and huge memory saving when using
        //  - getIdentity().copy() instead of keeping the ODocument instance
        //  - .reset() instead of recreating a ODocument

        // about 17260 vertices per second in vm in embedded mode (@25MB/s i/o rate and about 400MB ram usage)
        // about 61112 vertices per second in vm in in-memory mode

        // current implementation
        // about  7748 vertices per second in embedded mode (@15MB/s i/o rate and about 1,3GB ram usage)
        // about 17421 vertices per second in in-memory mode
    }
}
