package org.openengsb.core.ekb.persistence.jena.internal;

import java.util.Iterator;
import java.util.UUID;

import org.apache.jena.riot.RDFDataMgr;
import org.openengsb.core.api.context.ContextHolder;
import org.openengsb.core.ekb.api.ModelRegistry;
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
    private ModelRegistry modelRegistry;

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
        dataset.begin(ReadWrite.WRITE);

        Model model = dataset.getDefaultModel();
        String context = ContextHolder.get().getCurrentContextId();
        Property contextHeadProperty = model.getProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT);
        Resource contextRes = model.getResource(JenaConstants.CDL_NAMESPACE + context);
        Resource commit = model.getProperty(contextRes, contextHeadProperty).getObject().asResource();

        if (headRevision != null) {
            UUID revUUID = UUID.fromString(commit.getLocalName());
            System.out.println(revUUID);
            if (!revUUID.equals(headRevision)) {
                LOGGER.info("The head revision is not the same with the latest revision");
                return;
            }
        }

        System.out.println("delete done 1");

        Property commitParentRevision = model.getProperty(JenaConstants.CDL_COMMIT_PARENT_REVISION);
        Statement stmt = commit.getProperty(commitParentRevision);
        if (stmt == null) {
            model.removeAll(contextRes, null, null);
        } else {
            Resource prevCommit = stmt.getObject().asResource();
            contextRes.removeAll(contextHeadProperty);
            contextRes.addProperty(contextHeadProperty, prevCommit);
            model.remove(prevCommit, model.getProperty(JenaConstants.CDL_COMMIT_CHILD_REVISION), commit);
        }
        System.out.println("delete done 2");

        ExtendedIterator<Statement> iter = commit.listProperties(model.getProperty(JenaConstants.CDL_COMMIT_INSERTS))
                .andThen(commit.listProperties(model.getProperty(JenaConstants.CDL_COMMIT_UPDATES)));

        System.out.println("delete done 3");
        while (iter.hasNext()) {
            Resource node = iter.next().getObject().asResource();
            System.out.println("DEL: " + node.getURI());
            model.removeAll(node, null, null);
        }
        System.out.println("delete done 4");

        model.removeAll(commit, null, null);
        System.out.println("delete done 5");

        OwlHelper.save(model, "src/test/resources/test-delete.owl");

        dataset.commit();
        dataset.end();
    }

    @Override
    public UUID getCurrentRevisionNumber() {
        dataset.begin(ReadWrite.READ);

        Model model = dataset.getDefaultModel();
        String context = ContextHolder.get().getCurrentContextId();
        Resource contextRes = model.getResource(JenaConstants.CDL_NAMESPACE + context);
        Property contextHead = model.getProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT);
        Resource revision = model.getProperty(contextRes, contextHead).getObject().asResource();
        UUID revUUID = UUID.fromString(revision.getLocalName());
        dataset.end();

        return revUUID;
    }

    @Override
    public Object executeQuery(String string, String contextId) {

        // TODO
        return null;
    }

    @Override
    public UUID getLastRevisionNumberOfContext(String contextId) {
        dataset.begin(ReadWrite.READ);

        Model model = dataset.getDefaultModel();
        Resource contextRes = model.getResource(JenaConstants.CDL_NAMESPACE + contextId);
        Property contextHead = model.getProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT);
        RDFNode revision = model.getProperty(contextRes, contextHead).getObject();
        UUID revUUID = UUID.fromString(revision.asLiteral().getString());
        dataset.end();

        return revUUID;
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

        Model commitModel = commit.getDataGraph();

        Property oid = commitModel.getProperty(JenaConstants.CDL_OID);
        Property provWasRevisionOf = commitModel.getProperty(JenaConstants.PROV_REVISION);

        if (commit.getInserts() != null || !commit.getInserts().isEmpty()) {
            Iterator<RDFNode> inserts = commit.getInserts().iterator();

            while (inserts.hasNext()) {
                RDFNode insert = inserts.next();

                commitModel.add(commitInstance, insertProperty, insert);
                commitModel.add(commitInstance, entityProperty, insert);
            }
        }

        if (commit.getUpdates() != null || !commit.getUpdates().isEmpty()) {
            Iterator<RDFNode> updates = commit.getUpdates().iterator();

            while (updates.hasNext()) {
                Resource update = updates.next().asResource();
                Statement stmt = update.getProperty(oid);

                Filter<Statement> filter = new JenaOidFilter(oid, stmt.getObject());
                ExtendedIterator<Statement> iter = parentCommitInstance.listProperties(entityProperty).filterKeep(
                        filter);
                if (iter.hasNext()) {
                    // link to previous version of data & remove old object from
                    // the entity list
                    Resource oldObject = iter.next().getObject().asResource();
                    commitModel.add(update, provWasRevisionOf, oldObject);
                    commitModel.remove(commitInstance, entityProperty, oldObject);
                }

                commitModel.add(commitInstance, updateProperty, update);
                commitModel.add(commitInstance, entityProperty, update);
            }
        }

        if (commit.getDeletes() != null || !commit.getDeletes().isEmpty()) {
            Iterator<RDFNode> deleteModel = commit.getDeletes().iterator();

            while (deleteModel.hasNext()) {
                RDFNode delete = deleteModel.next();

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

    public JenaSnapshot loadCommit(UUID commitID) {
        JenaSnapshot snapshot = null;

        dataset.begin(ReadWrite.READ);
        Model model = dataset.getDefaultModel();

        Resource commitInstance = model.getResource(JenaConstants.CDL_NAMESPACE + commitID);
        if (model.containsResource(commitInstance)) {
            createSnapshot(commitInstance);
        }

        dataset.end();

        return snapshot;
    }

    private void createSnapshot(Resource commitInstance) {
        Model model = commitInstance.getModel();
        JenaSnapshot snapshot = new JenaSnapshot();

    }

    // public EKBCommit loadCommit(JenaCommit commit) {
    //
    // EKBCommit result = new EKBCommit();
    // Map<ModelDescription, Class<?>> cache = new HashMap<>();
    //
    // result.setRevisionNumber(commit.getRevision());
    // result.setComment(commit.getComment());
    // result.setParentRevisionNumber(commit.getParentRevision());
    // result.setDomainId(commit.getDomainId());
    // result.setConnectorId(commit.getConnectorId());
    // result.setInstanceId(commit.getInstanceId());
    //
    // for (Resource insert : commit.getInserts()) {
    // result.addInsert(createModelOfResource(insert, cache));
    // }
    // for (Resource update : commit.getUpdates()) {
    // result.addUpdate(createModelOfResource(update, cache));
    // }
    // for (String delete : commit.getDeletes()) {
    // dataset.begin(ReadWrite.READ);
    //
    // Model model = dataset.getDefaultModel();
    // Resource commitRes = model.getResource(JenaConstants.CDL_NAMESPACE +
    // commit.getRevision());
    // Property deleteList =
    // model.getProperty(JenaConstants.CDL_COMMIT_DELETES);
    // Property oid = model.getProperty(JenaConstants.CDL_OID);
    // ExtendedIterator<Statement> deleteListIter =
    // commitRes.listProperties(deleteList);
    // Statement stmt = deleteListIter.filterKeep(new JenaOidFilter(oid,
    // delete)).next();
    //
    // if (stmt != null && stmt.getObject().isResource()) {
    // Resource object = stmt.getObject().asResource();
    // result.addDelete(createModelOfResource(object, cache));
    // }
    //
    // dataset.end();
    // }
    //
    // return result;
    // }
    //
    // private Object createModelOfResource(Resource object,
    // Map<ModelDescription, Class<?>> cache) {
    // try {
    // ModelDescription description = getDescriptionFromObject(object);
    // Class<?> modelClass;
    // if (cache.containsKey(description)) {
    // modelClass = cache.get(description);
    // } else {
    // modelClass = modelRegistry.loadModel(description);
    // cache.put(description, modelClass);
    // }
    // return convertResourceToModel(modelClass, object);
    // } catch (IllegalArgumentException | ClassNotFoundException e) {
    // LOGGER.warn("Unable to create model of the object {}",
    // object.getLocalName(), e);
    // return null;
    // }
    // }
    //
    // private Object convertResourceToModel(Class<?> modelClass, Resource
    // object) {
    // // TODO Auto-generated method stub
    // return null;
    // }
    //
    // /**
    // * Extracts the required values to lookup a model class from the given
    // * EDBObject. If this object does not contain the required information, an
    // * IllegalArgumentException is thrown.
    // */
    // private ModelDescription getDescriptionFromObject(Resource obj) {
    // Property =
    //
    // // String modelName =
    // // obj.getPropertyResourceValue(JenaConstants.CDL_MODEL_TYPE);
    // // String modelVersion = obj.getString(EDBConstants.MODEL_TYPE_VERSION);
    // // if (modelName == null || modelVersion == null) {
    // // throw new IllegalArgumentException("The object " + obj.getOID() +
    // // " contains no model information");
    // // }
    // // return new ModelDescription(modelName, modelVersion);
    // return null;
    // }
    //
    // public void setModelRegistry(ModelRegistry modelRegistry) {
    // this.modelRegistry = modelRegistry;
    // }
}
