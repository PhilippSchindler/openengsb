package org.openengsb.core.ekb.persistence.jena.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import jline.internal.Log;

import org.openengsb.core.ekb.persistence.jena.internal.api.OntoException;
import org.openengsb.core.ekb.persistence.jena.internal.api.OwlHelper;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFVisitor;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class JenaCommit {
    protected final Model dataGraph;

    private final List<RDFNode> inserts;
    private final List<RDFNode> updates;
    private final List<RDFNode> deletes;

    private String committer;
    private String context;
    private UUID revision;

    private final RDFVisitor jv = new JenaVisitor();

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

    protected JenaCommit(Resource commitRes) {
        Model temp = commitRes.getModel();
        OwlHelper.save(temp, "src/test/resources/test-jenacommit.owl");
        dataGraph = ModelFactory.createDefaultModel();
        dataGraph.setNsPrefixes(temp.getNsPrefixMap());

        iterativeAdd(dataGraph, commitRes);
        OwlHelper.save(dataGraph, "src/test/resources/test-iterativeAdd.owl");

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

        setProperty(commitRes, JenaConstants.CDL_COMMIT_COMMITTER, "Committer", String.class);
        setProperty(commitRes, JenaConstants.CDL_COMMIT_CONTEXT, "Context", String.class);
        setProperty(commitRes, JenaConstants.CDL_COMMIT_REVISION, "Revision", UUID.class);
        setProperty(commitRes, JenaConstants.CDL_COMMIT_TIMESTAMP, "Timestamp", Calendar.class);
        setProperty(commitRes, JenaConstants.CDL_COMMIT_COMMENT, "Comment", String.class);
        setProperty(commitRes, JenaConstants.CDL_COMMIT_PARENT_REVISION, "ParentRevision", UUID.class);
        setProperty(commitRes, JenaConstants.CDL_COMMIT_CHILD_REVISION, "ChildRevision", UUID.class);
        setProperty(commitRes, JenaConstants.CDL_COMMIT_DOMAIN_ID, "DomainId", String.class);
        setProperty(commitRes, JenaConstants.CDL_COMMIT_CONNECTOR_ID, "ConnectorId", String.class);
        setProperty(commitRes, JenaConstants.CDL_COMMIT_INSTANCE_ID, "InstanceId", String.class);

    }

    public void setProperty(Resource commitRes, String propName, String property, Class<?> clazz) {
        Property prop = dataGraph.getProperty(propName);
        Statement stmt = commitRes.getProperty(prop);
        if (stmt != null) {
            RDFNode node = stmt.getObject();
            if (node != null) {
                Object obj = node.visitWith(jv);
                if (propName.equals(JenaConstants.CDL_COMMIT_PARENT_REVISION)
                        || propName.equals(JenaConstants.CDL_COMMIT_CHILD_REVISION)
                        || propName.equals(JenaConstants.CDL_COMMIT_REVISION)) {
                    obj = UUID.fromString((String) obj);
                }
                String methodName = "set" + property; // fieldName
                try {
                    Method m = JenaCommit.class.getMethod(methodName, clazz);
                    m.invoke(this, obj);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    Log.info("Fault in Reflection: setMethod is not correct", e);
                } catch (NoSuchMethodException nsme) {
                    Log.info("Fault in Reflection: there is no such method", nsme);
                }

                Log.info("setProperty: " + obj);
            }
        }
    }

    private void iterativeAdd(Model model, Resource commitRes) {
        Model temp = commitRes.getModel();
        StmtIterator iter = temp.listStatements(commitRes, null, (RDFNode) null);
        while (iter.hasNext()) {
            Statement stmt = iter.next();
            if (!model.contains(stmt)) {
                model.add(stmt);
                RDFNode node = stmt.getObject();
                if (node.isResource()) {
                    iterativeAdd(model, node.asResource());
                }
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
