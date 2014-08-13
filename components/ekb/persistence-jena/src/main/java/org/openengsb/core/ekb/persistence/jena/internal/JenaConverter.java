package org.openengsb.core.ekb.persistence.jena.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.openengsb.core.api.context.ContextHolder;
import org.openengsb.core.api.model.FileWrapper;
import org.openengsb.core.api.model.ModelWrapper;
import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.core.api.model.OpenEngSBModelEntry;
import org.openengsb.core.api.security.AuthenticationContext;
import org.openengsb.core.ekb.api.ConnectorInformation;
import org.openengsb.core.ekb.api.EKBCommit;
import org.openengsb.core.ekb.persistence.jena.internal.api.OwlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.XSD;

public class JenaConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JenaConverter.class);

    public static final String FILEWRAPPER_FILENAME_SUFFIX = ".filename";

    public JenaConverter() {

        LOGGER.info("OntoConverter Start! ");
    }

    /**
     * Converts the models of an EKBCommit to EDBObjects and return an object
     * which contains the three corresponding lists
     */
    public JenaCommit convertEKBCommit(EKBCommit commit, AuthenticationContext authContext) {
        ConnectorInformation information = commit.getConnectorInformation();
        JenaCommit result = new JenaCommit(ContextHolder.get().getCurrentContextId(),
                (String) authContext.getAuthenticatedPrincipal());

        result.setTimestamp(Calendar.getInstance());
        result.setConnectorId(information.getConnectorId());
        result.setDomainId(information.getDomainId());
        result.setInstanceId(information.getInstanceId());

        OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, result.getDataGraph());
        model.setNsPrefix("", JenaConstants.CDL_NAMESPACE);

        result.getInserts().addAll(convertModelsToJenaResources(model, commit.getInserts()));
        result.getUpdates().addAll(convertModelsToJenaResources(model, commit.getUpdates()));
        result.getDeletes().addAll(convertCommitDeletes(model, commit.getDeletes()));

        LOGGER.info("OntoConverter done converting commit: " + result.getRevision());
        // TODO: test purpose only
        OwlHelper.save(model, "src/test/resources/test-insert.owl");

        return result;
    }

    public List<RDFNode> convertCommitDeletes(OntModel model, List<OpenEngSBModel> deletes) {
        List<RDFNode> delList = new ArrayList<RDFNode>();

        if (deletes != null && !deletes.isEmpty()) {
            Iterator<OpenEngSBModel> iter = deletes.iterator();

            while (iter.hasNext()) {
                OpenEngSBModel instance = iter.next();
                String oid = ModelWrapper.wrap(instance).getCompleteModelOID();
                RDFNode node = ResourceFactory.createPlainLiteral(oid);
                delList.add(node);
            }
        }

        return delList;
    }

    /**
     * Convert a list of models to a list of EDBObjects (the version retrieving
     * is not considered here. This is done in the EDB directly).
     */
    public List<Resource> convertModelsToJenaResources(OntModel model, List<OpenEngSBModel> instances) {

        List<Resource> result = new ArrayList<Resource>();
        if (instances != null) {
            for (Object instance : instances) {
                result.addAll(convertModelToJenaResource(instance, model));
            }
        }
        return result;
    }

    /**
     * Converts an OpenEngSBModel object to an EDBObject (the version retrieving
     * is not considered here. This is done in the EDB directly).
     */
    public List<Resource> convertModelToJenaResource(Object instance, OntModel model) {
        if (!OpenEngSBModel.class.isAssignableFrom(instance.getClass())) {
            throw new IllegalArgumentException("This function need to get a model passed");
        }
        List<Resource> objects = new ArrayList<>();
        if (instance != null) {
            convertSubModel((OpenEngSBModel) instance, objects, model);
        }
        return objects;
    }

    /**
     * Recursive function to generate a list of EDBObjects out of a model
     * object.
     */
    private Resource convertSubModel(OpenEngSBModel instance, List<Resource> objects, OntModel model) {
        String oid = ModelWrapper.wrap(instance).getCompleteModelOID();

        OntClass provCls = model.createClass(JenaConstants.PROV_ENTITY);
        OntClass modelCls = model.createClass(JenaConstants.CDL_NAMESPACE + instance.getClass().getSimpleName());
        Individual object = model.createIndividual(JenaConstants.CDL_NAMESPACE + UUID.randomUUID(), modelCls);
        Property rdfsSubClass = model.createProperty(JenaConstants.RDFS_SUBCLASS);
        modelCls.setPropertyValue(rdfsSubClass, provCls);

        DatatypeProperty oidProp = model.createDatatypeProperty(JenaConstants.CDL_OID);
        object.addProperty(oidProp, oid);

        for (OpenEngSBModelEntry entry : instance.toOpenEngSBModelEntries()) {
            if (entry.getValue() == null) {
                continue;
            } else if (entry.getType().equals(FileWrapper.class)) {
                try {
                    FileWrapper wrapper = (FileWrapper) entry.getValue();
                    String content = Base64.encodeBase64String(wrapper.getContent());

                    DatatypeProperty datatypeProp = model.createDatatypeProperty(JenaConstants.CDL_NAMESPACE
                            + entry.getKey());
                    datatypeProp.addDomain(modelCls);
                    datatypeProp.addRange(XSD.xstring);

                    object.addProperty(datatypeProp, content);
                    object.addProperty(
                            model.createDatatypeProperty(JenaConstants.CDL_NAMESPACE + entry.getKey()
                                    + FILEWRAPPER_FILENAME_SUFFIX), wrapper.getFilename());
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }
            } else if (OpenEngSBModel.class.isAssignableFrom(entry.getType())) {
                OpenEngSBModel temp = (OpenEngSBModel) entry.getValue();
                Resource objProperty = convertSubModel(temp, objects, model);

                ObjectProperty objProp = model.createObjectProperty(JenaConstants.CDL_NAMESPACE + entry.getKey());
                objProp.addDomain(modelCls);
                objProp.addRange(model.createClass(JenaConstants.CDL_NAMESPACE + temp.getClass().getSimpleName()));

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
                        item = convertSubModel(oesbModel, objects, model);
                        property = model.createObjectProperty(JenaConstants.CDL_NAMESPACE + entry.getKey());
                        property.addRange(model.createClass(JenaConstants.CDL_NAMESPACE
                                + oesbModel.getClass().getSimpleName()));
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
                        item = convertSubModel((OpenEngSBModel) item, objects, model);
                    }

                    DatatypeProperty datatypeProp = model.createDatatypeProperty(JenaConstants.CDL_NAMESPACE
                            + entry.getKey());
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
                        key = convertSubModel((OpenEngSBModel) key, objects, model);
                    }
                    if (valueIsModel) {
                        value = convertSubModel((OpenEngSBModel) value, objects, model);
                    }
                    object.addProperty(model.createProperty(JenaConstants.CDL_NAMESPACE + entry.getKey()),
                            (Resource) key);
                    object.addProperty(model.createProperty(JenaConstants.CDL_NAMESPACE + entry.getKey()),
                            (Resource) value);
                }
            } else {
                // TODO: What type should be handled here?
                DatatypeProperty datatypeProp = model.createDatatypeProperty(JenaConstants.CDL_NAMESPACE
                        + entry.getKey());
                datatypeProp.addDomain(modelCls);

                if (entry.getType().equals(Integer.class)) {
                    datatypeProp.addRange(XSD.xint);
                    object.addLiteral(model.createProperty(JenaConstants.CDL_NAMESPACE + entry.getKey()),
                            entry.getValue());

                } else { // String
                    datatypeProp.addRange(XSD.xstring);
                    object.addProperty(datatypeProp, entry.getValue().toString());
                }
            }
        }

        OntClass modelInfoCls = model.createClass(JenaConstants.CDL_MODEL);
        Individual modelInfo = model.createIndividual(JenaConstants.CDL_MODEL + "_"
                + instance.getClass().getSimpleName(), modelInfoCls);

        Property modelProperty = model.createProperty(JenaConstants.CDL_HAS_MODEL);
        object.addProperty(modelProperty, modelInfo);

        DatatypeProperty modelTypeProp = model.createDatatypeProperty(JenaConstants.CDL_MODEL_TYPE);
        modelInfo.addProperty(modelTypeProp, instance.retrieveModelName());

        DatatypeProperty modelVersionProp = model.createDatatypeProperty(JenaConstants.CDL_MODEL_TYPE_VERSION);
        modelVersionProp.addDomain(modelInfoCls);
        modelVersionProp.addRange(XSD.xstring);
        modelInfo.addProperty(modelVersionProp, instance.retrieveModelVersion());

        objects.add(object);

        return object;
    }
}
