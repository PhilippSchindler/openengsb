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

package org.openengsb.core.ekb.persistence.jena.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import jline.internal.Log;

import org.apache.commons.io.FileUtils;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.openengsb.core.api.context.ContextHolder;
import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.core.api.model.OpenEngSBModelEntry;
import org.openengsb.core.api.model.QueryRequest;
import org.openengsb.core.api.security.AuthenticationContext;
import org.openengsb.core.ekb.api.EDBQueryFilter;
import org.openengsb.core.ekb.api.EKBCommit;
import org.openengsb.core.ekb.api.EKBService;
import org.openengsb.core.ekb.api.ModelRegistry;
import org.openengsb.core.ekb.api.Query;
import org.openengsb.core.ekb.api.SingleModelQuery;
import org.openengsb.core.ekb.api.hooks.EKBErrorHook;
import org.openengsb.core.ekb.api.hooks.EKBPostCommitHook;
import org.openengsb.core.ekb.api.hooks.EKBPreCommitHook;
import org.openengsb.core.ekb.persistence.jena.internal.models.SubModel;
import org.openengsb.core.ekb.persistence.jena.internal.models.TestModel;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;

@RunWith(MockitoJUnitRunner.class)
public class ContextCommitTest {
    private EKBService service;
    private JenaConverter ontoConverter;
    private AuthenticationContext authContext;
    private ModelRegistry registry;

    @Before
    public void setUp() {
        Dataset dataset = TDBFactory.createDataset("src/test/resources/tdb");
        Model model = RDFDataMgr.loadModel(JenaConstants.CDL_TEMPLATE);
        ontoConverter = new JenaConverter();

        // mockito stuff
        authContext = mock(AuthenticationContext.class);
        when(authContext.getAuthenticatedPrincipal()).thenReturn("Fajar");
        
        registry = new TestModelRegistry();
        JenaService ontoService = new JenaService(dataset, model, true);
        ontoService.setModelRegistry(registry);
        List<EKBPreCommitHook> preHooks = new ArrayList<EKBPreCommitHook>();
        List<EKBPostCommitHook> postHooks = new ArrayList<EKBPostCommitHook>();
        List<EKBErrorHook> errorHooks = new ArrayList<EKBErrorHook>();

        this.service = new EKBServiceJena(ontoService, ontoConverter, preHooks, postHooks, errorHooks, authContext);
        ContextHolder.get().setCurrentContextId("test");
    }
    
    @AfterClass
    public static void cleanUp() {
    	try {
			FileUtils.cleanDirectory(new File("src/test/resources/"));
		} catch (IOException e) {
			Log.info("Failed cleaning test directory", e);
		}
    }

    private EKBCommit createTestInsert() {
        EKBCommit commit = new EKBCommit();
        commit.setDomainId("testdomain").setConnectorId("testconnector").setInstanceId("testinstance");
        SubModel sb1 = new SubModel();
        sb1.setId("sb1");
        sb1.setValue("sb1value");
        SubModel sb2 = new SubModel();
        sb2.setId("sb2");
        sb2.setValue("sb2value");
        List<SubModel> sbs = new ArrayList<SubModel>();
        sbs.add(sb1);
        sbs.add(sb2);

        TestModel test = new TestModel();
        test.setId("A0");
        test.setName("A0_Fajar");
        test.setSubs(sbs);

        commit.addInsert(test);

        return commit;
    }

    private EKBCommit createTestInsert1() {
        EKBCommit commit = new EKBCommit();
        commit.setDomainId("testdomain").setConnectorId("testconnector").setInstanceId("testinstance");
        SubModel sb1 = new SubModel();
        sb1.setId("sb1");
        sb1.setValue("sb1value");
        SubModel sb2 = new SubModel();
        sb2.setId("sb2");
        sb2.setValue("sb2value");
        List<SubModel> sbs = new ArrayList<SubModel>();
        sbs.add(sb1);
        sbs.add(sb2);
        List<SubModel> sbs1 = new ArrayList<SubModel>();
        sbs1.add(sb1);

        TestModel test = new TestModel();
        test.setId("A1");
        test.setName("A1_Fajar");
        test.setSubs(sbs);
        TestModel test2 = new TestModel();
        test2.setId("A2");
        test2.setName("A2_Fajar");
        TestModel test3 = new TestModel();
        test3.setId("A3");
        test3.setName("A3_Fajar");
        test3.setSub(sb1);

        commit.addInsert(test);
        commit.addInsert(test2);
        commit.addInsert(test3);

        return commit;
    }

    private EKBCommit createTestInsert2() {
        EKBCommit commit = new EKBCommit();
        commit.setDomainId("testdomain").setConnectorId("testconnector").setInstanceId("testinstance");

        TestModel test = new TestModel();
        test.setId("A1");
        test.setName("A1_Fajar xx");
        TestModel test2 = new TestModel();
        test2.setId("A2");
        test2.setName("A2_Fajar xx");

        commit.addUpdate(test);
        commit.addUpdate(test2);

        return commit;
    }

    private EKBCommit createTestInsert3() {
        EKBCommit commit = new EKBCommit();
        commit.setDomainId("testdomain").setConnectorId("testconnector").setInstanceId("testinstance");

        TestModel test = new TestModel();
        test.setId("A4");
        test.setName("A4_Fajar YYY");

        TestModel test2 = new TestModel();
        test2.setId("A2");
        test2.setName("A2_Fajar YYY");

        commit.addInsert(test);
        commit.addUpdate(test2);

        return commit;
    }

    private EKBCommit createTestInsert4() {
        EKBCommit commit = new EKBCommit();
        commit.setDomainId("testdomain").setConnectorId("testconnector").setInstanceId("testinstance");

        TestModel test = new TestModel();
        test.setId("A2");

        commit.addDelete(test);

        return commit;
    }

    @Test
    public void testConvert() {
        ontoConverter.convertEKBCommit(createTestInsert(), authContext);
    }

    @Test
    public void testConvertEKBCommitToOntoTDB_shouldWork() throws Exception {
        service.commit(createTestInsert1());
        service.commit(createTestInsert2());
        service.commit(createTestInsert3());
        service.commit(createTestInsert4());
    }

    @Test
    public void testQuery_shouldWork() throws Exception {
        service.commit(createTestInsert1());
        Object obj = service.nativeQuery("select * where {?a rdf:type :TestModel} limit 100");
        System.out.println(obj);
    }

    @Test
    public void testGetRevision_shouldWork() {
        service.commit(createTestInsert1());
        UUID uuid = service.getLastRevisionId();
        Log.info("Test Head UUID revision", uuid);
    }

    @Test
    public void testLoadCommit_shouldWork() throws Exception {
        service.commit(createTestInsert1());
        service.commit(createTestInsert2());
        UUID uuid = service.getLastRevisionId();
        EKBCommit commit = service.loadCommit(uuid);
        Log.info(commit);
        Log.info(commit.getInserts());
        Log.info(commit.getUpdates());
        Log.info(commit.getDeletes());
        Log.info(commit.getRevisionNumber());
        Log.info(commit.getInstanceId());
        Log.info(commit.getDomainId());
        Log.info(commit.getConnectorId());
        Log.info(commit.getParentRevisionNumber());
        Log.info(commit.getConnectorInformation());
        Log.info(commit.getComment());
    }
    
    @Test
    public void testNonNativeQueryBuilder_ShouldWork() {
    	QueryRequest queryRequest = QueryRequest.create();
    	queryRequest.addParameter("id", "A2");
    	queryRequest.setModelClassName("TestModel");
    	queryRequest.caseInsensitive();
    	ParameterizedSparqlString str = JenaQueryRequestConverter.convertSimpleQueryRequest(queryRequest, null);
    	Log.info(str.toString());
    }
    
    @Test
    public void testNonNativeQuery_ShouldWork() {
    	service.commit(createTestInsert1());
    	service.commit(createTestInsert2());
    	
    	QueryRequest queryRequest = QueryRequest.create();
    	queryRequest.addParameter("id", "A1");
    	queryRequest.setModelClassName("TestModel");
    	queryRequest.caseInsensitive();
    	
    	Query query = new SingleModelQuery(TestModel.class, new EDBQueryFilter(queryRequest), null);
    	List<OpenEngSBModel> objs = service.query(query);
    	Iterator<OpenEngSBModel> objsIter = objs.iterator();
    	while(objsIter.hasNext()) {
    		OpenEngSBModel obj = objsIter.next();
    		Log.info("ID: ", obj.retrieveInternalModelId());
    		List<OpenEngSBModelEntry> entries = obj.toOpenEngSBModelValues();
    		Iterator<OpenEngSBModelEntry> entriesIter = entries.iterator();
    		while(entriesIter.hasNext()) {
    			OpenEngSBModelEntry entry = entriesIter.next();
    			Log.info(entry.getKey());
    			Log.info(entry.getValue()!=null ? entry.getValue() : "NULL");
    		}
    	}
    }

}
