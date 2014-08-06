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

    /**
     * Defines the string which represents the key for a model oid.
     */
    public static final String MODEL_OID = "modelId";

    /**
     * Defines the string which represents the key for a model version.
     */
    public static final String MODEL_VERSION = "modelVersion";

    /**
     * Defines the string which represents the key for a model timestamp.
     */
    public static final String MODEL_TIMESTAMP = "modelTimeStamp";

    /**
     * Defines the string which represents the key for the type of a model.
     */
    public static final String MODEL_TYPE = "modelType";

    /**
     * Defines the string which represents the key for the version of the
     * corresponding model type.
     */
    public static final String MODEL_TYPE_VERSION = "modelTypeVersion";

    public static final String CDL_NAMESPACE = "http://cdlflex.org/ekb#";

    // class link
    public static final String CDL_INFO_COMMIT = CDL_NAMESPACE + "InfoCommit";
    public static final String CDL_INFO_MODEL = CDL_NAMESPACE + "InfoModel";
    public static final String CDL_INFO_CONTEXT = CDL_NAMESPACE + "InfoContext";
    public static final String CDL_HAS_INFO_MODEL = CDL_NAMESPACE + "hasInfoModel";

    // commit class properties
    public static final String CDL_COMMIT_ID = CDL_NAMESPACE + "commitId";
    public static final String CDL_COMMIT_TIMESTAMP = CDL_NAMESPACE + "commitTime";
    public static final String CDL_COMMIT_CONNECTOR_ID = CDL_NAMESPACE + "connectorId";
    public static final String CDL_COMMIT_CONTEXT_ID = CDL_NAMESPACE + "contextId";
    public static final String CDL_COMMIT_DOMAIN_ID = CDL_NAMESPACE + "domainId";
    public static final String CDL_COMMIT_INSTANCE_ID = CDL_NAMESPACE + "instanceId";
    public static final String CDL_COMMIT_COMMITTER = CDL_NAMESPACE + "committer";

    public static final String CDL_COMMIT_NEXT = CDL_NAMESPACE + "nextInfoCommit";
    public static final String CDL_COMMIT_PREV = CDL_NAMESPACE + "prevInfoCommit";
    public static final String CDL_COMMIT_CONTEXT = CDL_NAMESPACE + "hasCommitContext";

    public static final String CDL_COMMIT_LIST_INSERT = CDL_NAMESPACE + "hasInserts";
    public static final String CDL_COMMIT_LIST_UPDATE = CDL_NAMESPACE + "hasUpdates";
    public static final String CDL_COMMIT_LIST_DELETE = CDL_NAMESPACE + "hasDeletes";
    public static final String CDL_COMMIT_LIST_ENTITY = CDL_NAMESPACE + "hasEntities";

    // context class properties
    public static final String CDL_CONTEXT_COMMIT = CDL_NAMESPACE + "hasContextCommit";
    public static final String CDL_CONTEXT_HEAD_COMMIT = CDL_NAMESPACE + "hasHeadCommit";
    public static final String CDL_CONTEXT_GRAPH = CDL_NAMESPACE + "contextGraph";
    public static final String CDL_CONTEXT_ID = CDL_NAMESPACE + "contextId";
    public static final String CDL_CONTEXT_START = CDL_NAMESPACE + "contextStart";

    // model class properties
    public static final String CDL_MODEL_TYPE = CDL_NAMESPACE + "modelType";
    public static final String CDL_MODEL_TYPE_VERSION = CDL_NAMESPACE + "modelTypeVersion";
    public static final String CDL_MODEL_NEXT = CDL_NAMESPACE + "nextInfoModel";

    // others
    public static final String CDL_OID = CDL_NAMESPACE + "oid";
    public static final String CDL_TEMPLATE = "src/main/resources/ekb.owl";

}
