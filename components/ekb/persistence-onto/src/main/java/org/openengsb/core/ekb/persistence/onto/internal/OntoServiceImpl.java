package org.openengsb.core.ekb.persistence.onto.internal;

import java.util.Iterator;
import java.util.UUID;

import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class OntoServiceImpl implements OntoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OntoServiceImpl.class);

    private final Dataset dataset;
    private final Model defaultModel;

    public OntoServiceImpl(Dataset dataset, Model defaultModel, Boolean isReset) {
        LOGGER.info("Initialize OntoService Dataset");
        if (defaultModel != null) {
            this.defaultModel = defaultModel;
        } else {
            this.defaultModel = RDFDataMgr.loadModel(OntoConstants.CDL_TEMPLATE);
        }
        this.dataset = dataset;
        if (isReset) {
            resetDataset();
            LOGGER.info("Reset OntoService Dataset");
        }
    }

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
    public OntResource commit(OntoCommit commit) {
        String contextId = commit.getContextId();
        UUID commitID = UUID.randomUUID();
        commit.setHeadRevisionNumber(commitID);

        String infoContextURI = OntoConstants.CDL_INFO_CONTEXT + contextId;
        String infoCommitURI = OntoConstants.CDL_INFO_COMMIT + commitID;

        dataset.begin(ReadWrite.WRITE);

        Model mainModel = dataset.getDefaultModel();
        OntModel ontDefModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, mainModel);
        Resource infoContextCls = mainModel.getResource(OntoConstants.CDL_INFO_CONTEXT);
        Resource infoCommitCls = mainModel.getResource(OntoConstants.CDL_INFO_COMMIT);
        Individual commitInstance = ontDefModel.createIndividual(infoCommitURI, infoCommitCls);

        Individual contextInstance = ontDefModel.getIndividual(infoContextURI);
        // System.out.println("eureka1!");

        if (!dataset.containsNamedModel(infoContextURI) || contextInstance == null) {
            // System.out.println("eureka2!");
            contextInstance = ontDefModel.createIndividual(infoContextURI, infoContextCls);
            contextInstance.addProperty(ontDefModel.createDatatypeProperty(OntoConstants.CDL_CONTEXT_START),
                    Long.toString(System.currentTimeMillis()));
            contextInstance.addProperty(ontDefModel.createDatatypeProperty(OntoConstants.CDL_CONTEXT_ID), contextId);
            contextInstance.addProperty(ontDefModel.createDatatypeProperty(OntoConstants.CDL_CONTEXT_GRAPH),
                    infoContextURI);

            Model temp = ModelFactory.createDefaultModel().add(defaultModel);
            temp.setNsPrefixes(defaultModel.getNsPrefixMap());
            dataset.addNamedModel(infoContextURI, temp);

            commit.setParentRevisionNumber(null);
        } else {

            ParameterizedSparqlString qString = new ParameterizedSparqlString();
            qString.setNsPrefix("", OntoConstants.CDL_NAMESPACE);
            qString.append("select ?a ?id where { ?a a :InfoCommit ." + " ?a :commitId ?id ."
                    + " ?a :hasCommitContext ?b ." + " FILTER NOT EXISTS { ?a :nextInfoCommit ?c } }");

            QueryExecution qe = QueryExecutionFactory.create(qString.asQuery(), ontDefModel);
            ResultSet rs = qe.execSelect();
            QuerySolution qs = rs.next();
            String parentCommitURI = qs.get("a").toString();
            String parentCommitId = qs.get("id").toString();
            commit.setParentRevisionNumber(UUID.fromString(parentCommitId));
            ontDefModel.getResource(parentCommitURI).addProperty(
                    ontDefModel.getProperty(OntoConstants.CDL_COMMIT_NEXT), commitInstance);
        }
        commitInstance.addProperty(ontDefModel.getProperty(OntoConstants.CDL_COMMIT_CONTEXT), contextInstance);
        contextInstance.addProperty(ontDefModel.getProperty(OntoConstants.CDL_CONTEXT_COMMIT), commitInstance);

        // OwlHelper.save(defaultModel, "main.owl");

        // --
        commitInstance.addProperty(ontDefModel.getProperty(OntoConstants.CDL_COMMIT_ID), commitID.toString());
        commitInstance.addProperty(ontDefModel.getProperty(OntoConstants.CDL_COMMIT_CONNECTOR_ID),
                commit.getConnectorId());
        commitInstance.addProperty(ontDefModel.getProperty(OntoConstants.CDL_COMMIT_TIMESTAMP), commit.getTimestamp()
                .toString());
        commitInstance.addProperty(ontDefModel.getProperty(OntoConstants.CDL_COMMIT_DOMAIN_ID), commit.getDomainId());
        commitInstance.addProperty(ontDefModel.getProperty(OntoConstants.CDL_COMMIT_INSTANCE_ID),
                commit.getInstanceId());

        Model contextModel = dataset.getNamedModel(infoContextURI);

        if (commit.getInsertModel() != null) {
            String insertID = OntoConstants.CDL_GRAPH_INSERT + commitID;
            commitInstance.addProperty(ontDefModel.getProperty(OntoConstants.CDL_COMMIT_GRAPH_INSERT), insertID);
            Model insertModel = commit.getInsertModel();
            contextModel.add(insertModel);

            dataset.addNamedModel(insertID, insertModel);

            // OwlHelper.save(insertModel, commitID + "-insert.owl");
        }
        if (commit.getUpdateModel() != null) {
            String updateID = OntoConstants.CDL_GRAPH_UPDATE + commitID;
            String beforeUpdateID = OntoConstants.CDL_GRAPH_BEFORE_UPDATE + commitID;
            commitInstance.addProperty(ontDefModel.getProperty(OntoConstants.CDL_COMMIT_GRAPH_BEFORE_UPDATE),
                    beforeUpdateID);
            commitInstance.addProperty(ontDefModel.getProperty(OntoConstants.CDL_COMMIT_GRAPH_UPDATE), updateID);

            OntModel updateModel = commit.getUpdateModel();
            Model beforeUpdateModel = processUpdate(contextModel, updateModel);
            contextModel.remove(beforeUpdateModel).add(updateModel);

            dataset.addNamedModel(updateID, updateModel);
            dataset.addNamedModel(beforeUpdateID, beforeUpdateModel);

            // OwlHelper.save(updateModel, commitID + "-update.owl");
            // OwlHelper.save(beforeUpdateModel, commitID +
            // "-before-update.owl");
        }
        if (commit.getDeleteModel() != null) {
            String deleteID = OntoConstants.CDL_GRAPH_DELETE + commitID;
            commitInstance.addProperty(ontDefModel.getProperty(OntoConstants.CDL_COMMIT_GRAPH_DELETE), deleteID);

            OntModel deleteModel = commit.getDeleteModel();
            Model beforeDeleteModel = processUpdate(contextModel, deleteModel);
            contextModel.remove(beforeDeleteModel);

            dataset.addNamedModel(deleteID, deleteModel);
        }

        // --
        OwlHelper.save(contextModel, contextId + ".owl");
        OwlHelper.save(ontDefModel, "MAIN.owl");

        dataset.commit();
        dataset.end();

        return null;
    }

    private Model processUpdate(Model contextModel, OntModel updateModel) {
        Model beforeUpdate = ModelFactory.createDefaultModel();
        beforeUpdate.setNsPrefixes(updateModel.getNsPrefixMap());
        Iterator<OntClass> listClasses = updateModel.listClasses();
        while (listClasses.hasNext()) {
            OntClass cls = listClasses.next();
            if (cls.getURI().equals(OntoConstants.CDL_INFO_MODEL)) {
                // TODO: model versioning stuff
                continue;
            }
            ExtendedIterator<Individual> instances = updateModel.listIndividuals(cls);
            while (instances.hasNext()) {
                Individual instance = instances.next();
                doUpdate(beforeUpdate, contextModel, instance);
            }
        }
        return beforeUpdate;
    }

    private void doUpdate(Model beforeUpdate, Model contextModel, Resource instance) {
        Resource oldInstance = contextModel.getResource(instance.getURI());
        beforeUpdate.add(oldInstance.listProperties());
    }
}
