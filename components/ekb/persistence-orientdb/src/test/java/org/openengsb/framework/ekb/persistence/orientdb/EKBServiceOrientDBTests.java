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
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.framework.ekb.persistence.orientdb.models.*;
import org.openengsb.framework.ekb.persistence.orientdb.visualization.DotExporter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public class EKBServiceOrientDBTests {

    private static EKBServiceOrientDB service;
    public static final boolean DO_GRAPHICAL_EXPORT = true;
    private static Random random;

    @Before
    public void setUp() throws IOException {
        createDatabaseAndSchema();
        service = new EKBServiceOrientDB();
        service.setDatabase(OrientDBHelper.getDefault().getConnection());
        service.setUIIDIndexSupportEnabled(true);
        random = new Random(4711);
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

        generator.addModel(Activity.class);
        generator.addModel(Project.class);
        generator.addModel(Person.class);
        generator.addModel(Manager.class);  // manager must be added after person due to inheritance

        generator.addModel(ComplexModel.class);
        generator.addModel(Plc.class);
        generator.addModel(Elp.class);

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

    private Person createTestPerson(int i) {
        String[] names = new String[]{ "Anna", "Bernd", "Claus", "Hans", "Tina" };
        String[] passwords = new String[]{ "12345", "qwerty", "qwertz", "abc123", "password" };
        String[] uiids = new String[]{ "akjha-askac", "1uiya-hask1", "1212a-45acw", "121ax-12789", "12nk1-mkdhn" };

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
        String[] desciptions = new String[]{ "design engine part 16.1", "evualuate max trust", "contact supplier" };
        String[] uiids = new String[]{ "mncn1-0askl", "hnnkm-1789a", "11212-00144" };
        int[] durations = new int[]{ 45, 0, 2 };
        int[] expDurations = new int[]{ 80, 40, 1 };
        boolean[] finished = new boolean[]{ false, false, true };

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

        assert (OType.isSimpleType(model.getSomeString()));
        assert (OType.isSimpleType(model.getSomeByte()));
        assert (OType.isSimpleType(model.getSomeShort()));
        assert (OType.isSimpleType(model.getSomeInteger()));
        assert (OType.isSimpleType(model.getSomeLong()));
        assert (OType.isSimpleType(model.getSomeFloat()));
        assert (OType.isSimpleType(model.getSomeDouble()));
        assert (OType.isSimpleType(model.getSomeDecimal()));
        assert (OType.isSimpleType(model.getSomeBoolean()));

        assertFalse(OType.isSimpleType(model.getSomeEmbeddedObject()));
        assertFalse(OType.isSimpleType(model.getSomeEmbeddedList()));
        assertFalse(OType.isSimpleType(model.getSomeEmbeddedSet()));
        assertFalse(OType.isSimpleType(model.getSomeEmbeddedMap()));

        ODocument document = service.getDatabase().newInstance("V");
        service.convertModel((OpenEngSBModel) model, document);

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
        model.setSomeBinary(new byte[]{ 1, 2, 4, 8, 16, 32, 64 });

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
    public void testCommitPerformance_add10000Persons() {
        EKBCommit c1 = createCommit();
        for (int i = 0; i < 10000; i++) {
            Person p = createTestPerson(0);
            p.setUiid(UUID.randomUUID().toString());
            c1.addOperation(new Operation(OperationType.INSERT, p));
        }

        long start = System.currentTimeMillis();
        service.commit(c1);
        long stop = System.currentTimeMillis();

        long time = stop - start;

        System.out.println("inserted 10000 persons in: " + time + "ms");
    }

    @Test
    public void testCommitPerformance_plcInParallel() {
        final int THREADS = 2;
        final int ENTITIES = 10000;
        Plc[] plcs = generateRandomPlcs(ENTITIES);
        Elp[] elps = generateRandomElps(ENTITIES);

        EKBCommit[] commitsInsertPlc = createParallelCommits(THREADS, ENTITIES, plcs, OperationType.INSERT);
        EKBCommit[] commitsInsertElp = createParallelCommits(THREADS, ENTITIES, elps, OperationType.INSERT);
        EKBCommit[] commitsUpdatePlc = createParallelCommits(THREADS, ENTITIES, plcs, OperationType.UPDATE);
        EKBCommit[] commitsDelete = createParallelCommits(THREADS, ENTITIES, plcs, OperationType.DELETE);

        long insertTime = runParallelCommits(commitsInsertPlc);
        System.out.println("inserted " + ENTITIES + " plc splitted into " + THREADS + " commits in " +
                insertTime + " ms");

        for(Plc plc : plcs) {
            modifyPlc(plc);
        }
        long updateTime = runParallelCommits(commitsUpdatePlc);
        System.out.println("updated  " + ENTITIES + " plc splitted into " + THREADS + " commits in " +
                updateTime + " ms");

        long insertTime2 = runParallelCommits(commitsInsertElp);
        System.out.println("inserted " + ENTITIES + " elp splitted into " + THREADS + " commits in " +
                insertTime2 + " ms");

        long deleteTime = runParallelCommits(commitsDelete);
        System.out.println("deleted  " + ENTITIES + " plc splitted into " + THREADS + " commits in " +
                deleteTime + " ms");


        /*
            index enabled

            one thread
            inserted 10000 plc splitted into 1 commits in 8379 ms
            updated  10000 plc splitted into 1 commits in 8971 ms
            inserted 10000 elp splitted into 1 commits in 6914 ms
            deleted  10000 plc splitted into 1 commits in 3557 ms

            two threads
            inserted 10000 plc splitted into 2 commits in 5685 ms
            updated  10000 plc splitted into 2 commits in 6237 ms
            inserted 10000 elp splitted into 2 commits in 4592 ms
            deleted  10000 plc splitted into 2 commits in 2364 ms

            four threads
            inserted 10000 plc splitted into 4 commits in 5391 ms
            updated  10000 plc splitted into 4 commits in 7394 ms
            inserted 10000 elp splitted into 4 commits in 4502 ms
            deleted  10000 plc splitted into 4 commits in 2772 ms
         */
    }

    @Test
    public void testCommitPerformance_plcSplitToSmallerCommits() {
        final int COMMITS = 1;
        final int ENTITIES = 10000;
        Plc[] plcs = generateRandomPlcs(ENTITIES);
        Elp[] elps = generateRandomElps(ENTITIES);
        Relationship[] relationships = generateRandomRelationships(plcs, elps, ENTITIES);

        EKBCommit[] commitsInsertPlc = createParallelCommits(COMMITS, ENTITIES, plcs, OperationType.INSERT);
        EKBCommit[] commitsInsertElp = createParallelCommits(COMMITS, ENTITIES, elps, OperationType.INSERT);
        EKBCommit[] commitsUpdatePlc = createParallelCommits(COMMITS, ENTITIES, plcs, OperationType.UPDATE);
        EKBCommit[] commitsDelete = createParallelCommits(COMMITS, ENTITIES, plcs, OperationType.DELETE);
        EKBCommit[] commitsInsertRelationships = createParallelCommits(COMMITS, ENTITIES, relationships,
                OperationType.INSERT_RELATIONSHIP);
        EKBCommit[] commitsDeleteRelationships = createParallelCommits(COMMITS, ENTITIES, relationships,
                OperationType.DELETE_RELATIONSHIP);


        long insertTime = runSequentialCommits(commitsInsertPlc);
        System.out.println("inserted " + ENTITIES + " plc splitted into " + COMMITS + " commits in " +
            insertTime + " ms");

        for(Plc plc : plcs) {
            modifyPlc(plc);
        }
        long updateTime = runSequentialCommits(commitsUpdatePlc);
        System.out.println("updated  " + ENTITIES + " plc splitted into " + COMMITS + " commits in " +
            updateTime + " ms");

        long insertTime2 = runSequentialCommits(commitsInsertElp);
        System.out.println("inserted " + ENTITIES + " elp splitted into " + COMMITS + " commits in " +
                insertTime2 + " ms");

        long insertRelTime = runSequentialCommits(commitsInsertRelationships);
        System.out.println("inserted " + ENTITIES + " relationships splitted into " + COMMITS + " commits in " +
                insertRelTime + " ms");

        long deleteRelTime = runSequentialCommits(commitsDeleteRelationships);
        System.out.println("deleted " + ENTITIES + " relationships splitted into " + COMMITS + " commits in " +
                deleteRelTime + " ms");

        long deleteTime = runSequentialCommits(commitsDelete);
        System.out.println("deleted  " + ENTITIES + " plc splitted into " + COMMITS + " commits in " +
            deleteTime + " ms");


        /*
            with index
            inserted 10000 plc splitted into 1 commits in            8173 ms
            updated  10000 plc splitted into 1 commits in            6310 ms
            inserted 10000 elp splitted into 1 commits in            7061 ms
            inserted 10000 relationships splitted into 1 commits in  8859 ms
            deleted  10000 relationships splitted into 1 commits in  4849 ms
            deleted  10000 plc splitted into 1 commits in            2474 ms

            without index
            inserted 10000 plc splitted into 1 commits in            7695 ms
            updated  10000 plc splitted into 1 commits in            6250 ms
            inserted 10000 elp splitted into 1 commits in            6826 ms
            inserted 10000 relationships splitted into 1 commits in  8740 ms
            deleted  10000 relationships splitted into 1 commits in  4428 ms
            deleted  10000 plc splitted into 1 commits in            2179 ms

            with massiveInsertIntent and index
            inserted 10000 plc splitted into 1 commits in            7802 ms
            updated  10000 plc splitted into 1 commits in           10869 ms
            inserted 10000 elp splitted into 1 commits in            6603 ms
            inserted 10000 relationships splitted into 1 commits in  9453 ms
            deleted  10000 relationships splitted into 1 commits in  9922 ms
            deleted  10000 plc splitted into 1 commits in            3901 ms
         */
    }



    public <T> EKBCommit[] createParallelCommits(final int THREADS, final int ENTITIES, T[] models,
            OperationType type) {
        EKBCommit[] commits = new EKBCommit[THREADS];
        for (int i = 0; i < THREADS; i++) {
            commits[i] = new EKBCommitImpl();
            for (int j = ENTITIES/THREADS * i; j < ENTITIES/THREADS * (i + 1); j++) {
                commits[i].addOperation(new Operation(type, models[j]));
            }
        }
        return commits;
    }




    public long runParallelCommits(EKBCommit[] commits) {
        long start = System.currentTimeMillis();

        Thread[] threads = new Thread[commits.length];
        for (int i = 0; i < commits.length; i++) {
            final EKBCommit c = commits[i];
            threads[i] = new Thread(new Runnable() {
                @Override public void run() {
                    EKBServiceOrientDB s = new EKBServiceOrientDB();
                    s.setDatabase(OrientDBHelper.getDefault().getConnection());
                    s.commit(c);
                }
            });
        }
        for (int i = 0; i < commits.length; i++) {
            threads[i].start();
        }
        for (int i = 0; i < commits.length; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long stop = System.currentTimeMillis();
        return stop - start;
    }
    public long runSequentialCommits(EKBCommit[] commits) {
        long start = System.currentTimeMillis();

        for (int i = 0; i < commits.length; i++) {
            service.commit(commits[i]);
        }

        long stop = System.currentTimeMillis();
        return stop - start;
    }


    private static Plc generateRandomPlc() {
        Plc plc = new Plc();
        final int FIRST_BITS = 50;
        final int SECOND_BITS = 50;
        final int CUSTOM_BITS = 50;
        final int BASE = 32;

        plc.setUuid(UUID.randomUUID().toString());
        plc.setProject(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setRegion(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setComponentNumber(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setCpuNumber(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setChannelName(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setRackId(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setPosition(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setKks0(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setKks1(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setKks2(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setKks3(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setLongText(new BigInteger(500, random).toString(BASE));
        plc.setStatus(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setDp(new BigInteger(FIRST_BITS, random).toString(BASE));
        plc.setCat(new BigInteger(FIRST_BITS, random).toString(BASE));

        plc.setPlt(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setMeasureRangeStart(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setMeasureRangeEnd(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setMeasureUnit(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setTextSwitchOn(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setInverted(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setTextSwitchOff(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setFunc(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setRange(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setDefaultRef(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setIoa1(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setIoa2(new BigInteger(SECOND_BITS, random).toString(BASE));
        plc.setIoa3(new BigInteger(SECOND_BITS, random).toString(BASE));

        plc.setCustom1(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom2(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom3(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom4(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom5(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom6(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom7(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom8(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom9(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom10(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom11(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom12(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom13(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom14(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom15(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom16(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom17(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom18(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom19(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom20(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom21(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom22(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom23(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom24(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom25(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom26(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom27(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom28(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom29(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom30(new BigInteger(CUSTOM_BITS, random).toString(BASE));

        return plc;
    }
    private static Plc[] generateRandomPlcs(int amount) {
        Plc[] plcs = new Plc[amount];
        for (int i = 0; i < amount; i++)
            plcs[i] = generateRandomPlc();
        return plcs;
    }

    private static Elp generateRandomElp() {
        Elp elp = new Elp();
        final int FIRST_BITS = 50;
        final int SECOND_BITS = 50;
        final int CUSTOM_BITS = 50;
        final int BASE = 32;

        elp.setUuid(UUID.randomUUID().toString());
        elp.setProject(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setRegion(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setComponentNumber(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setCpuNumber(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setChannelName(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setRackId(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setPosition(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setKks0(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setKks1(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setKks2(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setKks3(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setLongText(new BigInteger(500, random).toString(BASE));
        elp.setStatus(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setPhysicalPin(new BigInteger(FIRST_BITS, random).toString(BASE));
        elp.setPlacement(new BigInteger(FIRST_BITS, random).toString(BASE));


        elp.setSw(new BigInteger(SECOND_BITS, random).toString(BASE));
        elp.setPins(Arrays.asList(
                new BigInteger(SECOND_BITS, random).toString(BASE),
                new BigInteger(SECOND_BITS, random).toString(BASE),
                new BigInteger(SECOND_BITS, random).toString(BASE),
                new BigInteger(SECOND_BITS, random).toString(BASE),
                new BigInteger(SECOND_BITS, random).toString(BASE),
                new BigInteger(SECOND_BITS, random).toString(BASE),
                new BigInteger(SECOND_BITS, random).toString(BASE),
                new BigInteger(SECOND_BITS, random).toString(BASE),
                new BigInteger(SECOND_BITS, random).toString(BASE),
                new BigInteger(SECOND_BITS, random).toString(BASE)
        ));

        elp.setCustom1(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom2(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom3(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom4(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom5(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom6(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom7(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom8(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom9(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom10(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom11(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom12(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom13(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom14(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom15(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom16(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom17(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom18(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom19(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom20(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom21(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom22(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom23(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom24(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom25(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom26(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom27(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom28(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom29(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        elp.setCustom30(new BigInteger(CUSTOM_BITS, random).toString(BASE));

        return elp;
    }
    private static Elp[] generateRandomElps(int amount) {
        Elp[] elps = new Elp[amount];
        for (int i = 0; i < amount; i++)
            elps[i] = generateRandomElp();
        return elps;
    }


    private Relationship[] generateRandomRelationships(Plc[] pcls, Elp[] elps, int count) {
        Relationship[] relationships = new Relationship[count];
        for (int i = 0; i < count; i++)
            relationships[i] = new RelationshipImpl("related",
                pcls[random.nextInt(pcls.length)], elps[random.nextInt(pcls.length)]);
        return relationships;
    }

    private void modifyPlc(Plc plc) {
        final int FRIST_BITS = 100;
        final int CUSTOM_BITS = 150;
        final int BASE = 32;

        plc.setCpuNumber(new BigInteger(FRIST_BITS, random).toString(BASE));
        plc.setChannelName(new BigInteger(FRIST_BITS, random).toString(BASE));
        plc.setRackId(new BigInteger(FRIST_BITS, random).toString(BASE));
        plc.setPosition(new BigInteger(FRIST_BITS, random).toString(BASE));
        plc.setKks0(new BigInteger(FRIST_BITS, random).toString(BASE));
        plc.setKks1(new BigInteger(FRIST_BITS, random).toString(BASE));
        plc.setKks2(new BigInteger(FRIST_BITS, random).toString(BASE));
        plc.setKks3(new BigInteger(FRIST_BITS, random).toString(BASE));

        plc.setCustom1(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom2(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom3(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom4(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom5(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom6(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom7(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom8(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom9(new BigInteger(CUSTOM_BITS, random).toString(BASE));
        plc.setCustom10(new BigInteger(CUSTOM_BITS, random).toString(BASE));
    }

    // LIMITATION OF CURRENT RELATIONSHIP NAMING FOUND WHEN INSTANCES OF THE SAME MODEL ARE RELATED
    // for example a Person is related to another Person
    // this does not work right now because of the relationship link names being equal


}
