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

import org.openengsb.core.api.model.OpenEngSBModel;

public class Operation {

    private OperationType type;
    private OpenEngSBModel model;

    public OperationType getType() {
        return type;
    }
    public OpenEngSBModel getModel() {
        return model;
    }

    /**
     * Create a new operation for use in an EKBCommit.
     * Check's if model is a OpenEngSBModel and throws an IllegalArgumentException otherwise
     * @param type
     * @param model
     */
    public Operation(OperationType type, Object model) {
        this.type = type;
        checkIfModel(model);
        this.model = (OpenEngSBModel)model;
    }

    /**
     * Checks if an object is an OpenEngSBModel and throws an IllegalArgumentException if the object is no model.
     */
    private void checkIfModel(Object model) {
        if (!OpenEngSBModel.class.isAssignableFrom(model.getClass())) {
            throw new IllegalArgumentException("Only models can be committed");
        }
    }
}

