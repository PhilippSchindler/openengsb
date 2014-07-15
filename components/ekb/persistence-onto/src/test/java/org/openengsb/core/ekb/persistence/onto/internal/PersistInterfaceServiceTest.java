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

package org.openengsb.core.ekb.persistence.onto.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openengsb.core.api.context.ContextHolder;
import org.openengsb.core.api.model.ModelWrapper;
import org.openengsb.core.ekb.api.EKBCommit;
import org.openengsb.core.ekb.api.EKBService;
import org.openengsb.core.ekb.api.hooks.EKBErrorHook;
import org.openengsb.core.ekb.api.hooks.EKBPostCommitHook;
import org.openengsb.core.ekb.api.hooks.EKBPreCommitHook;
import org.openengsb.core.ekb.persistence.onto.internal.models.SubModel;
import org.openengsb.core.ekb.persistence.onto.internal.models.TestModel;
import org.openengsb.core.ekb.persistence.onto.internal.models.TestModel2;

import com.hp.hpl.jena.rdf.model.Resource;

public class PersistInterfaceServiceTest {
    private EKBService service;

    @Before
    public void setUp() {
        OntoService ontoService = mock(OntoService.class);
        OntoConverter ontoConverter = mock(OntoConverter.class);
        List<EKBPreCommitHook> preHooks = new ArrayList<EKBPreCommitHook>();
        List<EKBPostCommitHook> postHooks = new ArrayList<EKBPostCommitHook>();
        List<EKBErrorHook> errorHooks = new ArrayList<EKBErrorHook>();
        this.service = new EKBServiceOnto(ontoService, ontoConverter, preHooks, postHooks, errorHooks);
        ContextHolder.get().setCurrentContextId("test");
    }

    @Test
    public void testIfModelAgentIsSet_shouldWork() throws Exception {
        TestModel model = new TestModel();
        assertThat("TestModel isn't enhanced. Maybe you forgot to set the java agent?",
                ModelWrapper.isModel(model.getClass()), is(true));
    }

    @Test
    public void testIfRealModelsCanBeCommited_shouldWork() throws Exception {
        EKBCommit commit = new EKBCommit();
        commit.setDomainId("testdomain").setConnectorId("testconnector").setInstanceId("testinstance");
        commit.addInsert(new TestModel2());
        service.commit(commit);
    }

    @Test
    public void testIfRealModelsCanTransformed_shouldWork() throws Exception {
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

        EKBCommit commit = new EKBCommit();
        OntoConverter converter = new OntoConverter();
        commit.setDomainId("testdomain").setConnectorId("testconnector").setInstanceId("testinstance");
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
        OntoCommit oc = converter.convertEKBCommit(commit);
        Resource res = oc.getInserts().get(0);
        System.out.println(res.toString());

        OwlHelper.save(res.getModel(), "fajar.owl");
    }
}
