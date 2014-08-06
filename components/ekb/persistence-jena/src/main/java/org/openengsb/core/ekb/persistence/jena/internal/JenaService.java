package org.openengsb.core.ekb.persistence.jena.internal;

import java.util.Iterator;
import java.util.UUID;

import org.apache.jena.riot.RDFDataMgr;
import org.openengsb.core.ekb.persistence.jena.internal.api.OntoService;
import org.openengsb.core.ekb.persistence.jena.internal.api.OwlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
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

        String infoContextURI = JenaConstants.CDL_NAMESPACE + contextId;
        String infoCommitURI = JenaConstants.CDL_NAMESPACE + commitID;

        dataset.begin(ReadWrite.WRITE);

        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, dataset.getDefaultModel());

        Resource infoContextCls = ontModel.getResource(JenaConstants.CDL_INFO_CONTEXT);
        Resource infoCommitCls = ontModel.getResource(JenaConstants.CDL_INFO_COMMIT);

        Individual commitInstance = ontModel.createIndividual(infoCommitURI, infoCommitCls);
        Individual contextInstance = ontModel.getIndividual(infoContextURI);
        Resource parentCommitInstance = null;

        Property insertProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_LIST_INSERT);
        Property updateProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_LIST_UPDATE);
        Property deleteProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_LIST_DELETE);
        Property entityProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_LIST_ENTITY);

        if (!dataset.containsNamedModel(infoContextURI) || contextInstance == null) {
            contextInstance = ontModel.createIndividual(infoContextURI, infoContextCls);
            DatatypeProperty contextProp = ontModel.createDatatypeProperty(JenaConstants.CDL_CONTEXT_ID);
            DatatypeProperty graphProp = ontModel.createDatatypeProperty(JenaConstants.CDL_CONTEXT_GRAPH);
            contextInstance.addProperty(contextProp, contextId);
            contextInstance.addProperty(graphProp, infoContextURI);

            Model temp = ModelFactory.createDefaultModel();
            temp.setNsPrefixes(defaultModel.getNsPrefixMap());
            dataset.addNamedModel(infoContextURI, temp);
            commit.setParentRevision(null);

        } else {
            parentCommitInstance = contextInstance.getPropertyResourceValue(ontModel
                    .createProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT));
            parentCommitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_NEXT), commitInstance);

            commit.setParentRevision(UUID.fromString(parentCommitInstance.getLocalName()));

            NodeIterator nodeIter = ontModel.listObjectsOfProperty(parentCommitInstance, entityProperty);
            while (nodeIter.hasNext()) {
                ontModel.add(commitInstance, entityProperty, nodeIter.next());
            }

        }

        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_CONTEXT), contextInstance);

        contextInstance
                .setPropertyValue(ontModel.createProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT), commitInstance);

        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_ID), commitID.toString());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_COMMITTER), commit.getCommitter());
        commitInstance
                .addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_CONNECTOR_ID), commit.getConnectorId());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_DOMAIN_ID), commit.getDomainId());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_INSTANCE_ID), commit.getInstanceId());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_TIMESTAMP), commit.getTimestamp()
                .toString(), XSDDatatype.XSDlong);

        Model contextModel = dataset.getNamedModel(infoContextURI);
        Model commitModel = commit.getCommitGraph();

        Property rdfType = commitModel.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        Property oid = commitModel.getProperty(JenaConstants.CDL_OID);
        Property provWasRevisionOf = commitModel.getProperty("http://www.w3.org/ns/prov#wasRevisionOf");

        Resource provEntity = ontModel.getResource("http://www.w3.org/ns/prov#Entity");

        if (commit.getInserts() != null || !commit.getInserts().isEmpty()) {
            Iterator<Resource> inserts = commit.getInserts().iterator();

            while (inserts.hasNext()) {
                Resource insert = inserts.next();
                ontModel.add(insert, rdfType, provEntity);
                ontModel.add(commitModel.listStatements(insert, rdfType, (RDFNode) null));
                ontModel.add(commitModel.listStatements(insert, oid, (RDFNode) null));
                ontModel.add(commitInstance, insertProperty, insert);
                ontModel.add(commitInstance, entityProperty, insert);
            }
        }

        if (commit.getUpdates() != null || !commit.getUpdates().isEmpty()) {
            Iterator<Resource> updates = commit.getUpdates().iterator();

            while (updates.hasNext()) {
                Resource update = updates.next();
                Statement stmt = update.getProperty(oid);
                System.out.println("EEK: " + stmt.getObject());
                Filter<Statement> filter = new JenaOidFilter(oid, stmt.getObject());
                ExtendedIterator<Statement> iter = parentCommitInstance.listProperties(entityProperty).filterKeep(
                        filter);
                if (iter.hasNext()) {
                    // link to previous version of data & remove old object from
                    // entity list
                    Resource oldObject = iter.next().getSubject();
                    ontModel.add(update, provWasRevisionOf, oldObject);
                    ontModel.remove(commitInstance, entityProperty, oldObject);
                }
                ontModel.add(update, rdfType, provEntity);
                ontModel.add(commitModel.listStatements(update, rdfType, (RDFNode) null));
                ontModel.add(commitModel.listStatements(update, oid, (RDFNode) null));

                ontModel.add(commitInstance, updateProperty, update);
                ontModel.add(commitInstance, entityProperty, update);
            }
        }

        if (commit.getDeletes() != null || !commit.getDeletes().isEmpty()) {
            Iterator<Resource> deleteModel = commit.getDeletes().iterator();

            while (deleteModel.hasNext()) {
                Resource delete = deleteModel.next();
                RDFNode dataToBeDeleted = delete.getProperty(oid).getObject();

                ontModel.add(commitInstance, deleteProperty, dataToBeDeleted);
                ontModel.remove(commitInstance, entityProperty, dataToBeDeleted);
            }
        }

        contextModel.add(commit.getCommitGraph());

        OwlHelper.save(contextModel, "src/test/resources/test-data.owl");
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
