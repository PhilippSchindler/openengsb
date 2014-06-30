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

/**
 * Container for all constants used in the context of the EDB/EKB/Models.
 */
public final class OntoConstants {

    private OntoConstants() {
        // this class should not be instantiated
    }

    /**
     * Defines the string which represents the key for a model oid.
     */
    public static final String MODEL_OID = "ontoId";

    /**
     * Defines the string which represents the key for a model version.
     */
    public static final String MODEL_VERSION = "ontoVersion";

    /**
     * Defines the string which represents the key for a model timestamp.
     */
    public static final String MODEL_TIMESTAMP = "ontoTimestamp";

    /**
     * Defines the string which represents the key for the type of a model.
     */
    public static final String MODEL_TYPE = "modelType";

    /**
     * Defines the string which represents the key for the version of the
     * corresponding model type.
     */
    public static final String MODEL_TYPE_VERSION = "modelTypeVersion";
}
