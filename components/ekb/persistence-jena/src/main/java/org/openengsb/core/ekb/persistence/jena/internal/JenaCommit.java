package org.openengsb.core.ekb.persistence.jena.internal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.openengsb.core.ekb.persistence.jena.internal.api.OntoException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class JenaCommit {
    private final Model dataGraph;

    private final List<RDFNode> inserts;
    private final List<RDFNode> updates;
    private final List<RDFNode> deletes;

    private String committer;
    private String context;
    private UUID revision;

    private Boolean committed = false;
    private Calendar timestamp;
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

        inserts = new ArrayList<RDFNode>();
        updates = new ArrayList<RDFNode>();
        deletes = new ArrayList<RDFNode>();

        this.revision = UUID.randomUUID();
    }

    protected JenaCommit() {
        dataGraph = ModelFactory.createDefaultModel();
        inserts = new ArrayList<RDFNode>();
        updates = new ArrayList<RDFNode>();
        deletes = new ArrayList<RDFNode>();
    }

    /**
     * TODO
     * 
     * @param commitRes
     */
    protected JenaCommit(Resource commitRes) {
        Model temp = commitRes.getModel();
        dataGraph = ModelFactory.createDefaultModel();

        iterativeAdd(dataGraph, commitRes);

        inserts = new ArrayList<RDFNode>();
        updates = new ArrayList<RDFNode>();
        deletes = new ArrayList<RDFNode>();

        Property insProp = temp.getProperty(JenaConstants.CDL_COMMIT_INSERTS);
        Property updProp = temp.getProperty(JenaConstants.CDL_COMMIT_UPDATES);
        Property delProp = temp.getProperty(JenaConstants.CDL_COMMIT_DELETES);

        List<RDFNode> insertNodes = dataGraph.listObjectsOfProperty(insProp).toList();
        List<RDFNode> updateNodes = dataGraph.listObjectsOfProperty(updProp).toList();
        List<RDFNode> deleteNodes = dataGraph.listObjectsOfProperty(delProp).toList();

        inserts.addAll(insertNodes);
        updates.addAll(updateNodes);
        deletes.addAll(deleteNodes);

        /**
         * should use the RDFVisitor
         */
        // Property committerProp =
        // temp.getProperty(JenaConstants.CDL_COMMIT_COMMITTER);
        // if((String data = dataGraph.getProperty(commitRes,
        // committerProp).getObject()) != null) {
        // this.committer = dataGraph.getProperty(commitRes,
        // committerProp).getObject().asLiteral().getString();
        // }
        //
        // Property contextProp =
        // temp.getProperty(JenaConstants.CDL_COMMIT_CONTEXT);
        // this.context = dataGraph.getProperty(commitRes,
        // contextProp).getObject().asLiteral().getString();
        //
        // Property revisionProp =
        // temp.getProperty(JenaConstants.CDL_COMMIT_REVISION);
        // String rev = dataGraph.getProperty(commitRes,
        // revisionProp).getObject().asLiteral().getString();
        // this.revision = UUID.fromString(rev);
        //
        // Property timeStampProp =
        // temp.getProperty(JenaConstants.CDL_COMMIT_TIMESTAMP);
        // // String rev = dataGraph.getProperty(commitRes,
        // // revisionProp).getObject().;
        // // this.revision = UUID.fromString(rev);
        //
        // Property commentProp =
        // temp.getProperty(JenaConstants.CDL_COMMIT_COMMENT);
        // this.comment = dataGraph.getProperty(commitRes,
        // commentProp).getObject().asLiteral().getString();
        //
        // Property parentProp =
        // temp.getProperty(JenaConstants.CDL_COMMIT_PARENT_REVISION);
        // String parentRev = dataGraph.getProperty(commitRes,
        // parentProp).getObject().asLiteral().getString();
        // this.revision = UUID.fromString(parentRev);
        //
        // Property childProp =
        // temp.getProperty(JenaConstants.CDL_COMMIT_CHILD_REVISION);
        // String childRev = dataGraph.getProperty(commitRes,
        // childProp).getObject().asLiteral().getString();
        // if(this.revision = UUID.fromString(childRev);
        //
        // Property domainProp =
        // temp.getProperty(JenaConstants.CDL_COMMIT_DOMAIN_ID);
        // Property connectorProp =
        // temp.getProperty(JenaConstants.CDL_COMMIT_CONNECTOR_ID);
        // Property instanceProp =
        // temp.getProperty(JenaConstants.CDL_COMMIT_INSTANCE_ID);

    }

    // TODO:
    private String getStringValue(Model model, Resource resource, String link) {
        Property prop = model.getProperty(link);
        model.listObjectsOfProperty(resource, prop);

        return link;
    }

    private void iterativeAdd(Model model, Resource commitRes) {
        Model temp = commitRes.getModel();
        StmtIterator iter = temp.listStatements(commitRes, null, (RDFNode) null);
        model.add(iter.toList());
        while (iter.hasNext()) {
            Statement stmt = iter.next();
            RDFNode node = stmt.getObject();
            if (node.isResource()) {
                iterativeAdd(model, node.asResource());
            }
        }
    }

    public void insert(RDFNode obj) throws OntoException {
        inserts.add(obj);
    }

    public void update(RDFNode obj) throws OntoException {
        updates.add(obj);
    }

    public void delete(RDFNode oid) throws OntoException {
        deletes.add(oid);
    }

    public Model getDataGraph() {
        return dataGraph;
    }

    public List<RDFNode> getInserts() {
        return inserts;
    }

    public List<RDFNode> getUpdates() {
        return updates;
    }

    public List<RDFNode> getDeletes() {
        return deletes;
    }

    public String getCommitter() {
        return committer;
    }

    public void setCommitter(String committer) {
        this.committer = committer;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public UUID getRevision() {
        return revision;
    }

    public void setRevision(UUID revision) {
        this.revision = revision;
    }

    public void setCommitted(Boolean committed) {
        this.committed = committed;
    }

    public Calendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Calendar timestamp) {
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

    public Boolean getCommitted() {
        return committed;
    }

}
