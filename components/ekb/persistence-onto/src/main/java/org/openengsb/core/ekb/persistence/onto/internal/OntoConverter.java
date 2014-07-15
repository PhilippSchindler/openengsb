package org.openengsb.core.ekb.persistence.onto.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.openengsb.core.api.context.ContextHolder;
import org.openengsb.core.api.model.FileWrapper;
import org.openengsb.core.api.model.ModelWrapper;
import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.core.api.model.OpenEngSBModelEntry;
import org.openengsb.core.ekb.api.ConnectorInformation;
import org.openengsb.core.ekb.api.EKBCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.XSD;

public class OntoConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntoConverter.class);

    public static final String FILEWRAPPER_FILENAME_SUFFIX = ".filename";
    public static final String CDL_ONTOLOGY = "http://cdl.ifs.tuwien.ac.at/jena";
    public static final String CDL_NAMESPACE = "http://cdl.ifs.tuwien.ac.at/jena#";
    public static final String CDL_COMMIT_TIMESTAMP = CDL_NAMESPACE + "commitTime";
    public static final String CDL_CONNECTOR_ID = CDL_NAMESPACE + "connectorId";
    public static final String CDL_CONTEXT_ID = CDL_NAMESPACE + "contextId";
    public static final String CDL_DOMAIN_ID = CDL_NAMESPACE + "domainId";
    public static final String CDL_INSTANCE_ID = CDL_NAMESPACE + "instanceId";
    public static final String CDL_COMMIT_INFO = CDL_NAMESPACE + "commitInfo_";
    public static final String CDL_HAS_COMMIT_INFO = CDL_NAMESPACE + "hasCommitInfo";

    public EKBCommit convertOntoCommit(OntoCommit commit) {
        EKBCommit ekbCommit = new EKBCommit();

        return ekbCommit;
    }

    /**
     * Converts the models of an EKBCommit to EDBObjects and return an object
     * which contains the three corresponding lists
     */
    public OntoCommit convertEKBCommit(EKBCommit commit) {
        ConnectorInformation information = commit.getConnectorInformation();
        OntoCommit result = new OntoCommit();
        OntModel model = ModelFactory.createOntologyModel();
        model.setNsPrefix("", CDL_NAMESPACE);

        Long timeStamp = System.currentTimeMillis();

        OntClass commitInfoCls = model.createClass(CDL_NAMESPACE + "CommitInfo");
        Individual commitInfo = model.createIndividual(CDL_COMMIT_INFO + timeStamp, commitInfoCls);

        AnnotationProperty annoProp = model.createAnnotationProperty(CDL_NAMESPACE + "hasCommitInfo");
        annoProp.setRange(commitInfoCls);
        Individual res = model.createIndividual(CDL_ONTOLOGY,
                model.createOntResource("http://www.w3.org/2002/07/owl#Ontology"));
        res.addProperty(annoProp, commitInfo);

        DatatypeProperty timestamp = createDatatypeProperty(model, CDL_COMMIT_TIMESTAMP, commitInfoCls, XSD.xstring);
        DatatypeProperty contextID = createDatatypeProperty(model, CDL_CONTEXT_ID, commitInfoCls, XSD.xstring);
        DatatypeProperty connectorID = createDatatypeProperty(model, CDL_CONNECTOR_ID, commitInfoCls, XSD.xstring);
        DatatypeProperty domainID = createDatatypeProperty(model, CDL_DOMAIN_ID, commitInfoCls, XSD.xstring);
        DatatypeProperty instanceID = createDatatypeProperty(model, CDL_INSTANCE_ID, commitInfoCls, XSD.xstring);

        commitInfo.addProperty(timestamp, timeStamp.toString());
        commitInfo.addProperty(contextID, ContextHolder.get().getCurrentContextId());
        commitInfo.addProperty(connectorID, information.getConnectorId());
        commitInfo.addProperty(domainID, information.getDomainId());
        commitInfo.addProperty(instanceID, information.getInstanceId());

        result.setInserts(convertModelsToJenaResources(commit.getInserts(), commitInfo, model));
        result.setUpdates(convertModelsToJenaResources(commit.getUpdates(), commitInfo, model));
        result.setDeletes(convertModelsToJenaResources(commit.getDeletes(), commitInfo, model));
        result.setModel(model);

        model.write(System.out);

        return result;
    }

    /**
     * Convert a list of models to a list of EDBObjects (the version retrieving
     * is not considered here. This is done in the EDB directly).
     */
    public List<Resource> convertModelsToJenaResources(List<OpenEngSBModel> instances, Resource info, OntModel model) {
        List<Resource> result = new ArrayList<>();
        if (instances != null) {
            for (Object instance : instances) {
                result.addAll(convertModelToJenaResource(instance, info, model));
            }
        }
        return result;
    }

    /**
     * Converts an OpenEngSBModel object to an EDBObject (the version retrieving
     * is not considered here. This is done in the EDB directly).
     */
    public List<Resource> convertModelToJenaResource(Object instance, Resource info, OntModel model) {
        if (!OpenEngSBModel.class.isAssignableFrom(instance.getClass())) {
            throw new IllegalArgumentException("This function need to get a model passed");
        }
        List<Resource> objects = new ArrayList<>();
        if (instance != null) {
            convertSubModel((OpenEngSBModel) instance, objects, info, model);
        }
        return objects;
    }

    /**
     * Recursive function to generate a list of EDBObjects out of a model
     * object.
     */
    private Resource convertSubModel(OpenEngSBModel instance, List<Resource> objects, Resource info, OntModel model) {
        String oid = ModelWrapper.wrap(instance).getCompleteModelOID();

        // System.out.println("DEM: " + oid);
        // System.out.println("DEM: " + instance.getClass().getSimpleName());

        OntClass modelCls = model.createClass(CDL_NAMESPACE + instance.getClass().getSimpleName());
        Individual object = model.createIndividual(CDL_NAMESPACE + oid, modelCls);

        for (OpenEngSBModelEntry entry : instance.toOpenEngSBModelEntries()) {
            if (entry.getValue() == null) {
                continue;
            } else if (entry.getType().equals(FileWrapper.class)) {
                try {
                    FileWrapper wrapper = (FileWrapper) entry.getValue();
                    String content = Base64.encodeBase64String(wrapper.getContent());

                    DatatypeProperty datatypeProp = model.createDatatypeProperty(CDL_NAMESPACE + entry.getKey());
                    datatypeProp.addDomain(modelCls);
                    datatypeProp.addRange(XSD.xstring);

                    object.addProperty(datatypeProp, content);
                    object.addProperty(
                            model.createDatatypeProperty(CDL_NAMESPACE + entry.getKey() + FILEWRAPPER_FILENAME_SUFFIX),
                            wrapper.getFilename());
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }
            } else if (OpenEngSBModel.class.isAssignableFrom(entry.getType())) {
                OpenEngSBModel temp = (OpenEngSBModel) entry.getValue();
                Resource objProperty = convertSubModel(temp, objects, info, model);

                ObjectProperty objProp = model.createObjectProperty(CDL_NAMESPACE + entry.getKey());
                objProp.addDomain(modelCls);
                objProp.addRange(model.createClass(CDL_NAMESPACE + temp.getClass().getSimpleName()));

                object.addProperty(objProp, objProperty);
            } else if (List.class.isAssignableFrom(entry.getType())) {
                List<?> list = (List<?>) entry.getValue();
                if (list == null || list.size() == 0) {
                    continue;
                }
                Boolean modelItems = null;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    ObjectProperty property = null;
                    if (modelItems == null) {
                        modelItems = OpenEngSBModel.class.isAssignableFrom(item.getClass());
                    }
                    if (modelItems) {
                        OpenEngSBModel oesbModel = (OpenEngSBModel) item;
                        item = convertSubModel(oesbModel, objects, info, model);
                        property = model.createObjectProperty(CDL_NAMESPACE + entry.getKey());
                        property.addRange(model.createClass(CDL_NAMESPACE + oesbModel.getClass().getSimpleName()));
                    }
                    property.addDomain(modelCls);
                    object.addProperty(property, (Resource) item);
                }
            } else if (entry.getType().isArray()) {
                Object[] array = (Object[]) entry.getValue();
                if (array == null || array.length == 0) {
                    continue;
                }
                Boolean modelItems = null;
                for (int i = 0; i < array.length; i++) {
                    Object item = array[i];
                    if (modelItems == null) {
                        modelItems = OpenEngSBModel.class.isAssignableFrom(item.getClass());
                    }
                    if (modelItems) {
                        item = convertSubModel((OpenEngSBModel) item, objects, info, model);
                    }

                    DatatypeProperty datatypeProp = model.createDatatypeProperty(CDL_NAMESPACE + entry.getKey());
                    datatypeProp.addDomain(modelCls);

                    object.addProperty(datatypeProp, (Resource) item);
                }
            } else if (Map.class.isAssignableFrom(entry.getType())) {
                Map<?, ?> map = (Map<?, ?>) entry.getValue();
                if (map == null || map.size() == 0) {
                    continue;
                }
                Boolean keyIsModel = null;
                Boolean valueIsModel = null;

                for (Map.Entry<?, ?> ent : map.entrySet()) {
                    if (keyIsModel == null) {
                        keyIsModel = OpenEngSBModel.class.isAssignableFrom(ent.getKey().getClass());
                    }
                    if (valueIsModel == null) {
                        valueIsModel = OpenEngSBModel.class.isAssignableFrom(ent.getValue().getClass());
                    }
                    Object key = ent.getKey();
                    Object value = ent.getValue();
                    if (keyIsModel) {
                        key = convertSubModel((OpenEngSBModel) key, objects, info, model);
                    }
                    if (valueIsModel) {
                        value = convertSubModel((OpenEngSBModel) value, objects, info, model);
                    }
                    object.addProperty(model.createProperty(CDL_NAMESPACE + entry.getKey()), (Resource) key);
                    object.addProperty(model.createProperty(CDL_NAMESPACE + entry.getKey()), (Resource) value);
                }
            } else {
                // TODO: What type should be handled here?
                DatatypeProperty datatypeProp = model.createDatatypeProperty(CDL_NAMESPACE + entry.getKey());
                datatypeProp.addDomain(modelCls);

                if (entry.getType().equals(Integer.class)) {
                    datatypeProp.addRange(XSD.xint);
                    object.addLiteral(model.createProperty(CDL_NAMESPACE + entry.getKey()), entry.getValue());

                } else { // String
                    datatypeProp.addRange(XSD.xstring);
                    object.addProperty(datatypeProp, entry.getValue().toString());
                }
            }
        }

        OntClass modelInfoCls = model.createClass(CDL_NAMESPACE + "ModelInfo");
        Individual modelInfo = model.createIndividual(CDL_NAMESPACE + "ModelInfo_"
                + instance.getClass().getSimpleName(), modelInfoCls);

        AnnotationProperty annoProp = model.createAnnotationProperty(CDL_NAMESPACE + "hasModelInfo");
        annoProp.setRange(modelInfoCls);
        modelCls.addProperty(annoProp, modelInfo);

        DatatypeProperty modelTypeProp = model.createDatatypeProperty(CDL_NAMESPACE + OntoConstants.MODEL_TYPE);
        modelTypeProp.addDomain(modelInfoCls);
        modelTypeProp.addRange(XSD.xstring);
        modelInfo.addProperty(modelTypeProp, instance.retrieveModelName());

        DatatypeProperty modelVersionProp = model.createDatatypeProperty(CDL_NAMESPACE
                + OntoConstants.MODEL_TYPE_VERSION);
        modelVersionProp.addDomain(modelInfoCls);
        modelVersionProp.addRange(XSD.xstring);
        modelInfo.addProperty(modelVersionProp, instance.retrieveModelVersion());

        objects.add(object);

        return object;
    }

    private DatatypeProperty createDatatypeProperty(OntModel model, String URI, Resource domain, Resource range) {

        DatatypeProperty modelTypeProp = model.createDatatypeProperty(URI);
        modelTypeProp.addDomain(domain);
        modelTypeProp.addRange(range);

        return modelTypeProp;
    }
}
