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

/**
 * Container for all constants used in the context of the EDB/EKB/Models.
 */
public final class JenaConstants {

    private JenaConstants() {
        // this class should not be instantiated
    }

    // namespaces
    public static final String CDL_NAMESPACE = "http://cdlflex.org/ekb#";
    public static final String PROV_NAMESPACE = "http://www.w3.org/ns/prov#";
    public static final String RDF_NAMESPACE = "http://www.w3.org/2002/07/owl#";
    public static final String RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";

    // concepts
    public static final String CDL_COMMIT = CDL_NAMESPACE + "Commit";
    public static final String CDL_MODEL = CDL_NAMESPACE + "Model";
    public static final String CDL_CONTEXT = CDL_NAMESPACE + "Context";

    // annotation property
    public static final String CDL_HAS_MODEL = CDL_NAMESPACE + "hasModel";

    // commit class properties
    public static final String CDL_COMMIT_REVISION = CDL_NAMESPACE + "revision";
    public static final String CDL_COMMIT_COMMITTER = CDL_NAMESPACE + "committer";
    public static final String CDL_COMMIT_DOMAIN_ID = CDL_NAMESPACE + "domainId";
    public static final String CDL_COMMIT_CONNECTOR_ID = CDL_NAMESPACE + "connectorId";
    public static final String CDL_COMMIT_INSTANCE_ID = CDL_NAMESPACE + "instanceId";
    public static final String CDL_COMMIT_COMMENT = CDL_NAMESPACE + "comment";
    public static final String CDL_COMMIT_TIMESTAMP = PROV_NAMESPACE + "startedAtTime";
    public static final String CDL_COMMIT_CHILD_REVISION = CDL_NAMESPACE + "hasChildRevision";
    public static final String CDL_COMMIT_PARENT_REVISION = CDL_NAMESPACE + "hasParentRevision";
    public static final String CDL_COMMIT_CONTEXT = CDL_NAMESPACE + "hasCommitContext";
    public static final String CDL_COMMIT_INSERTS = CDL_NAMESPACE + "hasInserts";
    public static final String CDL_COMMIT_UPDATES = CDL_NAMESPACE + "hasUpdates";
    public static final String CDL_COMMIT_DELETES = CDL_NAMESPACE + "hasDeletes";
    public static final String CDL_COMMIT_ENTITIES = CDL_NAMESPACE + "hasEntities";

    // context class properties
    public static final String CDL_CONTEXT_HEAD_COMMIT = CDL_NAMESPACE + "hasHeadCommit";
    public static final String CDL_CONTEXT_ID = CDL_NAMESPACE + "contextId";

    // model class properties
    public static final String CDL_MODEL_TYPE = CDL_NAMESPACE + "modelType";
    public static final String CDL_MODEL_TYPE_VERSION = CDL_NAMESPACE + "modelTypeVersion";
    public static final String CDL_MODEL_PARENT_MODEL = CDL_NAMESPACE + "hasParentModel";

    // generated primary key
    public static final String CDL_OID = CDL_NAMESPACE + "oid";

    // owl template
    public static final String CDL_TEMPLATE = "src/main/resources/ekb.owl";

    // model instance properties
    public static final String PROV_REVISION = PROV_NAMESPACE + "wasRevisionOf";
    public static final String PROV_ENTITY = PROV_NAMESPACE + "Entity";
    public static final String RDF_TYPE = RDF_NAMESPACE + "type";
    public static final String RDFS_SUBCLASS = RDFS_NAMESPACE + "subClassOf";

}
