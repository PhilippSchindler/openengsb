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

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class OntoConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntoConverter.class);

    public static final String FILEWRAPPER_FILENAME_SUFFIX = ".filename";
    public static final String CDL_NAMESPACE = "http://cdl.ifs.tuwien.ac.at/jena#";
    public static final String CDL_CONNECTOR_ID = CDL_NAMESPACE + "connectorId";
    public static final String CDL_CONTEXT_ID = CDL_NAMESPACE + "contextId";
    public static final String CDL_DOMAIN_ID = CDL_NAMESPACE + "domainId";
    public static final String CDL_INSTANCE_ID = CDL_NAMESPACE + "instanceId";

    /**
     * Converts the models of an EKBCommit to EDBObjects and return an object
     * which contains the three corresponding lists
     */
    public OntoCommit convertEKBCommit(EKBCommit commit) {
        ConnectorInformation information = commit.getConnectorInformation();
        OntoCommit result = new OntoCommit();
        result.setInserts(convertModelsToEDBObjects(commit.getInserts(), information));
        result.setUpdates(convertModelsToEDBObjects(commit.getUpdates(), information));
        result.setDeletes(convertModelsToEDBObjects(commit.getDeletes(), information));
        return result;
    }

    /**
     * Convert a list of models to a list of EDBObjects (the version retrieving
     * is not considered here. This is done in the EDB directly).
     */
    public List<Resource> convertModelsToEDBObjects(List<OpenEngSBModel> models, ConnectorInformation info) {
        List<Resource> result = new ArrayList<>();
        if (models != null) {
            for (Object model : models) {
                result.addAll(convertModelToEDBObject(model, info));
            }
        }
        return result;
    }

    /**
     * Converts an OpenEngSBModel object to an EDBObject (the version retrieving
     * is not considered here. This is done in the EDB directly).
     */
    public List<Resource> convertModelToEDBObject(Object model, ConnectorInformation info) {
        if (!OpenEngSBModel.class.isAssignableFrom(model.getClass())) {
            throw new IllegalArgumentException("This function need to get a model passed");
        }
        List<Resource> objects = new ArrayList<>();
        if (model != null) {
            convertSubModel((OpenEngSBModel) model, objects, info);
        }
        return objects;
    }

    /**
     * Recursive function to generate a list of EDBObjects out of a model
     * object.
     */
    private Resource convertSubModel(OpenEngSBModel model, List<Resource> objects, ConnectorInformation info) {
        String contextId = ContextHolder.get().getCurrentContextId();
        String oid = ModelWrapper.wrap(model).getCompleteModelOID();

        Resource object = ResourceFactory.createResource(CDL_NAMESPACE + contextId + oid);

        for (OpenEngSBModelEntry entry : model.toOpenEngSBModelEntries()) {
            if (entry.getValue() == null) {
                continue;
            } else if (entry.getType().equals(FileWrapper.class)) {
                try {
                    FileWrapper wrapper = (FileWrapper) entry.getValue();
                    String content = Base64.encodeBase64String(wrapper.getContent());
                    object.addProperty(ResourceFactory.createProperty(CDL_NAMESPACE + entry.getKey()), content);
                    object.addProperty(ResourceFactory.createProperty(CDL_NAMESPACE + entry.getKey()
                            + FILEWRAPPER_FILENAME_SUFFIX), wrapper.getFilename());
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }
            } else if (OpenEngSBModel.class.isAssignableFrom(entry.getType())) {
                OpenEngSBModel temp = (OpenEngSBModel) entry.getValue();
                Resource objProperty = convertSubModel(temp, objects, info);
                object.addProperty(ResourceFactory.createProperty(CDL_NAMESPACE + entry.getKey()), objProperty);
            } else if (List.class.isAssignableFrom(entry.getType())) {
                List<?> list = (List<?>) entry.getValue();
                if (list == null || list.size() == 0) {
                    continue;
                }
                Boolean modelItems = null;
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (modelItems == null) {
                        modelItems = OpenEngSBModel.class.isAssignableFrom(item.getClass());
                    }
                    if (modelItems) {
                        item = convertSubModel((OpenEngSBModel) item, objects, info);
                    }
                    object.addProperty(ResourceFactory.createProperty(CDL_NAMESPACE + entry.getKey()), (Resource) item);
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
                        item = convertSubModel((OpenEngSBModel) item, objects, info);
                    }
                    object.addProperty(ResourceFactory.createProperty(CDL_NAMESPACE + entry.getKey()), (Resource) item);
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
                        key = convertSubModel((OpenEngSBModel) key, objects, info);
                    }
                    if (valueIsModel) {
                        value = convertSubModel((OpenEngSBModel) value, objects, info);
                    }
                    object.addProperty(ResourceFactory.createProperty(CDL_NAMESPACE + entry.getKey()), (Resource) key);
                    object.addProperty(ResourceFactory.createProperty(CDL_NAMESPACE + entry.getKey()), (Resource) value);
                }
            } else {
                // TODO: What type should be handled here?
                if (entry.getType().equals(Integer.class) || entry.getType().equals(Long.class)
                        || entry.getType().equals(Double.class) || entry.getType().equals(Float.class)
                        || entry.getType().equals(Character.class) || entry.getType().equals(Boolean.class)) {
                    object.addLiteral(ResourceFactory.createProperty(CDL_NAMESPACE + entry.getKey()), entry.getValue());
                } else { // String
                    object.addProperty(ResourceFactory.createProperty(CDL_NAMESPACE + entry.getKey()), entry.getValue()
                            .toString());
                }
            }
        }

        object.addProperty(ResourceFactory.createProperty(CDL_NAMESPACE + OntoConstants.MODEL_TYPE),
                model.retrieveModelName());
        object.addProperty(ResourceFactory.createProperty(CDL_NAMESPACE + OntoConstants.MODEL_TYPE_VERSION),
                model.retrieveModelVersion());
        object.addProperty(ResourceFactory.createProperty(CDL_CONTEXT_ID), contextId);
        object.addProperty(ResourceFactory.createProperty(CDL_CONNECTOR_ID), info.getConnectorId());
        object.addProperty(ResourceFactory.createProperty(CDL_CONTEXT_ID), info.getDomainId());
        object.addProperty(ResourceFactory.createProperty(CDL_CONTEXT_ID), info.getInstanceId());

        objects.add(object);

        return object;
    }
}
