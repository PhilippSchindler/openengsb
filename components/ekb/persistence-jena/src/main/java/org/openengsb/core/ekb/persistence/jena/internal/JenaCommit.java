package org.openengsb.core.ekb.persistence.jena.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.openengsb.core.ekb.persistence.jena.internal.api.OntoException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

public class JenaCommit {
    private final Model dataGraph;

    private final List<Resource> inserts;
    private final List<Resource> updates;
    private final List<Resource> deletes;

    private final String committer;
    private final String context;
    private final UUID revision;

    private Boolean committed = false;
    private Long timestamp;
    private String comment;
    private UUID parentRevision;
    private UUID childRevision;
    private String domainId;
    private String connectorId;
    private String instanceId;

    /**
     * Create a new JenaCommit
     * 
     * @param committer commiterID
     * @param contextId contextID
     */
    public JenaCommit(String contextId, String committer) {

        this.dataGraph = ModelFactory.createDefaultModel();
        this.committer = committer;
        this.context = contextId;

        inserts = new ArrayList<Resource>();
        updates = new ArrayList<Resource>();
        deletes = new ArrayList<Resource>();

        this.revision = UUID.randomUUID();
    }

    public void insert(Resource obj) throws OntoException {
        inserts.add(obj);
    }

    public void update(Resource obj) throws OntoException {
        updates.add(obj);
    }

    public void delete(Resource oid) throws OntoException {
        deletes.add(oid);
    }

    public Boolean getCommitted() {
        return committed;
    }

    public void setCommitted(Boolean committed) {
        this.committed = committed;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public UUID getParentRevision() {
        return parentRevision;
    }

    public void setParentRevision(UUID parentRevision) {
        this.parentRevision = parentRevision;
    }

    public UUID getChildRevision() {
        return childRevision;
    }

    public void setChildRevision(UUID childRevision) {
        this.childRevision = childRevision;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Model getCommitGraph() {
        return dataGraph;
    }

    public List<Resource> getInserts() {
        return inserts;
    }

    public List<Resource> getUpdates() {
        return updates;
    }

    public List<Resource> getDeletes() {
        return deletes;
    }

    public String getCommitter() {
        return committer;
    }

    public String getContext() {
        return context;
    }

    public UUID getRevision() {
        return revision;
    }

}
