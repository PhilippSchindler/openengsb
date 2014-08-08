package org.openengsb.core.ekb.persistence.jena.internal;

import java.util.Calendar;
import java.util.Iterator;
import java.util.UUID;

import org.apache.jena.riot.RDFDataMgr;
import org.openengsb.core.ekb.persistence.jena.internal.api.OntoService;
import org.openengsb.core.ekb.persistence.jena.internal.api.OwlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;

public class JenaService implements OntoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JenaService.class);

    private final Dataset dataset;
    private final Model defaultModel;

    /**
     * Create a new JenaService.
     * 
     * @param dataset Main Storage.
     * @param defaultModel Root model containing information about context &
     *        commit.
     * @param isReset whether the existing data have to be reset.
     */
    public JenaService(Dataset dataset, Model defaultModel, Boolean isReset) {
        LOGGER.info("Initialize OntoService Dataset");
        if (defaultModel != null) {
            this.defaultModel = defaultModel;
        } else {
            this.defaultModel = RDFDataMgr.loadModel(JenaConstants.CDL_TEMPLATE);
        }
        this.dataset = dataset;
        if (isReset) {
            resetDataset();
            LOGGER.info("Reset OntoService Dataset");
        }
    }

    /**
     * Erase everything in the ontology. Proceed with your own risk!
     */
    private void resetDataset() {
        dataset.begin(ReadWrite.WRITE);

        Iterator<String> namedGraphs = dataset.listNames();
        while (namedGraphs.hasNext()) {
            String namedGraph = namedGraphs.next();
            dataset.removeNamedModel(namedGraph);
        }
        Model model = dataset.getDefaultModel();
        model.removeAll();
        model.add(defaultModel);
        model.setNsPrefixes(defaultModel.getNsPrefixMap());

        dataset.commit();
        dataset.end();
    }

    @Override
    public void deleteCommit(UUID headRevision) {
        // TODO Auto-generated method stub

    }

    @Override
    public UUID getCurrentRevisionNumber() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object executeQuery(String string, String contextId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UUID getLastRevisionNumberOfContext(String contextId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OntResource commit(JenaCommit commit) {
        String contextId = commit.getContext();
        UUID commitID = commit.getRevision();
        commit.setChildRevision(null);

        String contextURI = JenaConstants.CDL_NAMESPACE + contextId;
        String commitURI = JenaConstants.CDL_NAMESPACE + commitID;

        dataset.begin(ReadWrite.WRITE);

        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, dataset.getDefaultModel());

        Resource infoContextCls = ontModel.getResource(JenaConstants.CDL_CONTEXT);
        Resource infoCommitCls = ontModel.getResource(JenaConstants.CDL_COMMIT);

        Individual commitInstance = ontModel.createIndividual(commitURI, infoCommitCls);
        Individual contextInstance = ontModel.getIndividual(contextURI);
        Resource parentCommitInstance = null;

        Property insertProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_INSERTS);
        Property updateProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_UPDATES);
        Property deleteProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_DELETES);
        Property entityProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_ENTITIES);

        if (contextInstance == null) {
            contextInstance = ontModel.createIndividual(contextURI, infoContextCls);
            DatatypeProperty contextProp = ontModel.createDatatypeProperty(JenaConstants.CDL_CONTEXT_ID);
            contextInstance.addProperty(contextProp, contextId);

            commit.setParentRevision(null);

        } else {
            parentCommitInstance = contextInstance.getPropertyResourceValue(ontModel
                    .createProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT));
            parentCommitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_CHILD_REVISION),
                    commitInstance);
            commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_PARENT_REVISION),
                    parentCommitInstance);
            commit.setParentRevision(UUID.fromString(parentCommitInstance.getLocalName()));
            NodeIterator nodeIter = ontModel.listObjectsOfProperty(parentCommitInstance, entityProperty);
            while (nodeIter.hasNext()) {
                ontModel.add(commitInstance, entityProperty, nodeIter.next());
            }

        }

        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_CONTEXT), contextInstance);

        contextInstance
                .setPropertyValue(ontModel.createProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT), commitInstance);

        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_REVISION), commitID.toString());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_COMMITTER), commit.getCommitter());
        commitInstance
                .addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_CONNECTOR_ID), commit.getConnectorId());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_DOMAIN_ID), commit.getDomainId());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_INSTANCE_ID), commit.getInstanceId());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_TIMESTAMP),
                ontModel.createTypedLiteral(commit.getTimestamp()));

        Model commitModel = commit.getCommitGraph();

        Property oid = commitModel.getProperty(JenaConstants.CDL_OID);
        Property provWasRevisionOf = commitModel.getProperty(JenaConstants.PROV_REVISION);

        if (commit.getInserts() != null || !commit.getInserts().isEmpty()) {
            Iterator<Resource> inserts = commit.getInserts().iterator();

            while (inserts.hasNext()) {
                Resource insert = inserts.next();

                commitModel.add(commitInstance, insertProperty, insert);
                commitModel.add(commitInstance, entityProperty, insert);
            }
        }

        if (commit.getUpdates() != null || !commit.getUpdates().isEmpty()) {
            Iterator<Resource> updates = commit.getUpdates().iterator();

            while (updates.hasNext()) {
                Resource update = updates.next();
                Statement stmt = update.getProperty(oid);

                Filter<Statement> filter = new JenaOidFilter(oid, stmt.getObject());
                ExtendedIterator<Statement> iter = parentCommitInstance.listProperties(entityProperty).filterKeep(
                        filter);
                if (iter.hasNext()) {
                    // link to previous version of data & remove old object from
                    // the entity list
                    Resource oldObject = iter.next().getSubject();
                    commitModel.add(update, provWasRevisionOf, oldObject);
                    commitModel.remove(commitInstance, entityProperty, oldObject);
                }

                commitModel.add(commitInstance, updateProperty, update);
                commitModel.add(commitInstance, entityProperty, update);
            }
        }

        if (commit.getDeletes() != null || !commit.getDeletes().isEmpty()) {
            Iterator<String> deleteModel = commit.getDeletes().iterator();

            while (deleteModel.hasNext()) {
                String delete = deleteModel.next();

                Filter<Statement> filter = new JenaOidFilter(oid, delete);
                ExtendedIterator<Statement> iter = parentCommitInstance.listProperties(entityProperty).filterKeep(
                        filter);

                if (iter.hasNext()) {
                    Resource dataToBeDeleted = iter.next().getSubject();
                    commitModel.add(commitInstance, deleteProperty, dataToBeDeleted);
                    commitModel.remove(commitInstance, entityProperty, dataToBeDeleted);
                }
            }
        }

        ontModel.add(commitModel);

        OwlHelper.save(ontModel, "src/test/resources/test-main.owl");

        dataset.commit();
        dataset.end();

        return null;
    }

    public static class JenaOidFilter extends Filter<Statement> {
        private final Property p;
        private final RDFNode oid;

        public JenaOidFilter(Property p, RDFNode oid) {
            this.p = p;
            this.oid = oid;
        }

        public JenaOidFilter(Property p, String oid) {
            this.p = p;
            this.oid = ResourceFactory.createPlainLiteral(oid);
        }

        @Override
        public boolean accept(Statement o) {
            Resource obj = o.getObject().asResource();
            if (obj.getProperty(p).getObject().equals(oid)) {
                return true;
            } else {
                return false;
            }
        }
    }
}
