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

import java.util.*;

public class EKBCommitImpl implements EKBCommit {

    private Map<OperationType, List<Operation>> operations;

    private String domainId;
    private String connectorId;
    private String instanceId;
    private String comment;
    private UUID revisionNumber;
    private UUID parentRevisionNumber;

    public EKBCommitImpl() {
        this.operations = new HashMap<>();
        for (OperationType type : OperationType.values()) {
            operations.put(type, new ArrayList<Operation>());
        }
    }

    @Override
    public EKBCommit addOperation(Operation operation) {
        this.operations.get(operation.getType()).add(operation);
        return this;
    }

    @Override
    public EKBCommit addOperations(List<Operation> operations) {
        for (Operation operation : operations) {
            this.operations.get(operation.getType()).add(operation);
        }
        return this;
    }

    @Override
    public List<Operation> getOperations(OperationType type) {
        return operations.get(type);
    }

    @Override
    public EKBCommit setDomainId(String domainId) {
        this.domainId = domainId;
        return this;
    }

    @Override
    public EKBCommit setConnectorId(String connectorId) {
        this.connectorId = connectorId;
        return this;
    }

    @Override
    public EKBCommit setInstanceId(String instanceId) {
        this.instanceId = instanceId;
        return this;
    }

    @Override
    public String getDomainId() {
        return domainId;
    }

    @Override
    public String getConnectorId() {
        return connectorId;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public ConnectorInformation getConnectorInformation() {
        return new ConnectorInformation(domainId, connectorId, instanceId);
    }

    @Override
    public UUID getParentRevisionNumber() {
        return parentRevisionNumber;
    }

    @Override
    public void setParentRevisionNumber(UUID parentRevisionNumber) {
        this.parentRevisionNumber = parentRevisionNumber;
    }

    @Override
    public UUID getRevisionNumber() {
        return revisionNumber;
    }

    @Override
    public void setRevisionNumber(UUID revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

}
