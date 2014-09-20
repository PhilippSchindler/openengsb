package org.openengsb.framework.ekb.persistence.orientdb;

import org.openengsb.core.ekb.api.ConnectorInformation;

import java.util.*;

/**
 * Created by Philipp Schindler on 20.09.2014.
 */
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
        for (OperationType type : OperationType.values())
            operations.put(type, new ArrayList<Operation>());
    }

    @Override
    public EKBCommit addOperation(Operation operation) {
        this.operations.get(operation.getType()).add(operation);
        return this;
    }

    @Override
    public EKBCommit addOperations(List<Operation> operations) {
        for (Operation operation : operations)
            this.operations.get(operation.getType()).add(operation);
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
