package org.openengsb.core.ekb.persistence.onto.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class OntoCommit implements OntoCommitInterface {

    private List<Resource> inserts;
    private List<Resource> updates;
    private List<Resource> deletes;

    private Boolean committed = false;
    private String committer;
    private Long timestamp;
    private String context;
    private String comment;
    private String revision;
    private String parent;
    private String domainId;
    private String connectorId;
    private String instanceId;

    @Override
    public List<Resource> getInserts() {
        return inserts;
    }

    public void setInserts(List<Resource> inserts) {
        this.inserts = inserts;
    }

    @Override
    public List<Resource> getUpdates() {
        return updates;
    }

    public void setUpdates(List<Resource> updates) {
        this.updates = updates;
    }

    public List<Resource> getDeletes() {
        return deletes;
    }

    public void setDeletes(List<Resource> deletes) {
        this.deletes = deletes;
    }

    @Override
    public void insert(Resource obj) throws OntoException {
        if (inserts == null) {
            inserts = new ArrayList<Resource>();
        }
        inserts.add(obj);
    }

    @Override
    public void update(Resource obj) throws OntoException {
        if (updates == null) {
            updates = new ArrayList<Resource>();
        }
        updates.add(obj);
    }

    @Override
    public void delete(String oid) throws OntoException {
        Resource obj = ResourceFactory.createResource(OntoConverter.CDL_NAMESPACE + oid);
        if (deletes == null) {
            deletes = new ArrayList<Resource>();
        }
        deletes.add(obj);
    }

    @Override
    public List<Resource> getObjects() {
        List<Resource> objects = new ArrayList<Resource>();
        objects.addAll(inserts);
        objects.addAll(updates);
        return objects;
    }

    @Override
    public List<String> getDeletions() {
        List<String> objs = new ArrayList<>();
        Iterator<Resource> rs = deletes.iterator();
        while (rs.hasNext()) {
            Resource res = rs.next();
            objs.add(res.getLocalName());
        }
        return objs;
    }

    @Override
    public String getCommitter() {
        return committer;
    }

    @Override
    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getContextId() {
        return context;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void setCommitted(Boolean committed) {
        this.committed = committed;
    }

    @Override
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public UUID getRevisionNumber() {
        return revision != null ? UUID.fromString(revision) : null;
    }

    @Override
    public UUID getParentRevisionNumber() {
        return parent != null ? UUID.fromString(parent) : null;
    }

    @Override
    public void setHeadRevisionNumber(UUID head) {
        this.parent = head != null ? head.toString() : null;
    }

    @Override
    public String getDomainId() {
        return domainId;
    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public String getConnectorId() {
        return connectorId;
    }

    @Override
    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
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
