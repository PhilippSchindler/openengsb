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

import org.openengsb.core.ekb.api.ConnectorInformation;

import java.util.List;
import java.util.UUID;

public interface EKBCommit {

    /**
     * Adds an operation which should be executing when this commit object is actually committed.
     */
    EKBCommit addOperation(Operation operation);

    /**
     * Adds an operations which should be executing when this commit object is actually committed.
     */
    EKBCommit addOperations(List<Operation> operations);

    /**
     * Returns the list of Operations of the specified OperationType in this commit.
     */
    List<Operation> getOperations(OperationType type);

    /**
     * Defines the id of the domain from where the commit comes from.
     */
    EKBCommit setDomainId(String domainId);

    /**
     * Defines the id of the connector from where the commit comes from.
     */
    EKBCommit setConnectorId(String connectorId);

    /**
     * Defines the id of the instance from where the commit comes from.
     */
    EKBCommit setInstanceId(String instanceId);

    /**
     * Returns the id of the domain from where the commit comes from.
     */
    String getDomainId();

    /**
     * Returns the id of the connector from where the commit comes from.
     */
    String getConnectorId();

    /**
     * Returns the id of the instance from where the commit comes from.
     */
    String getInstanceId();

    /**
     * Gets the information about domain, connector and instance of an EKBCommit object and returns the corresponding
     * ConnectorInformation object.
     */
    ConnectorInformation getConnectorInformation();

    UUID getParentRevisionNumber();

    void setParentRevisionNumber(UUID parentRevisionNumber);

    UUID getRevisionNumber();

    void setRevisionNumber(UUID revisionNumber);

    String getComment();

    void setComment(String comment);
}
