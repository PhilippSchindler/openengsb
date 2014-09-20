package org.openengsb.framework.ekb.persistence.orientdb;

import org.openengsb.core.ekb.api.ConnectorInformation;
import java.util.List;
import java.util.UUID;

/**
 * Created by Philipp Schindler on 20.09.2014.
 */
public interface EKBCommit {

    /**
     * Adds an operation which should be executing when this commit object is actually committed.
     */
    public EKBCommit addOperation(Operation operation);

    /**
     * Adds an operations which should be executing when this commit object is actually committed.
     */
    public EKBCommit addOperations(List<Operation> operations);

    /**
     * Returns the list of Operations of the specified OperationType in this commit.
     */
    public List<Operation> getOperations(OperationType type);


    /**
     * Defines the id of the domain from where the commit comes from.
     */
    public EKBCommit setDomainId(String domainId);

    /**
     * Defines the id of the connector from where the commit comes from.
     */
    public EKBCommit setConnectorId(String connectorId);


    /**
     * Defines the id of the instance from where the commit comes from.
     */
    public EKBCommit setInstanceId(String instanceId);


    /**
     * Returns the id of the domain from where the commit comes from.
     */
    public String getDomainId();

    /**
     * Returns the id of the connector from where the commit comes from.
     */
    public String getConnectorId();

    /**
     * Returns the id of the instance from where the commit comes from.
     */
    public String getInstanceId();

    /**
     * Gets the information about domain, connector and instance of an EKBCommit object and returns the corresponding
     * ConnectorInformation object.
     */
    public ConnectorInformation getConnectorInformation();

    public UUID getParentRevisionNumber();

    public void setParentRevisionNumber(UUID parentRevisionNumber);

    public UUID getRevisionNumber();

    public void setRevisionNumber(UUID revisionNumber);

    public String getComment();

    public void setComment(String comment);
}
