package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */
public class SchemaGenerator {

    private ODatabaseDocumentTx database;
    private OSchema schema;

    private OClass V;
    private OClass E;
    private OClass commit;
    private OClass entity;
    private OClass history;
    private OClass revision;

    List<Class<?>> models;

    public SchemaGenerator(){
        models = new ArrayList<Class<?>>();
    }

    public SchemaGenerator(ODatabaseDocumentTx database) {
        this();
        this.database = database;
    }

    public ODatabaseDocumentTx getDatabase() {
        return database;
    }

    public void setDatabase(ODatabaseDocumentTx database) {
        this.database = database;
    }

    public void generateVersioningSchema()
    {
        // TODO some addtional infos/changes for handling versioning of edges

        schema = database.getMetadata().getSchema();

        V        = schema.getClass("V");
        E        = schema.getClass("E");
        commit   = schema.createClass("Commit", V);
        entity   = schema.createAbstractClass("Entity", V);
        history  = schema.createAbstractClass("History", V);
        revision = schema.createAbstractClass("Revision", V);

        commit.createProperty("timestamp", OType.DATETIME);
        commit.createProperty("inserts", OType.LINKLIST, revision);
        commit.createProperty("updates", OType.LINKLIST, revision);
        commit.createProperty("deletes", OType.LINKLIST, revision);
        commit.createProperty("parent", OType.LINK, commit);
        commit.createProperty("next", OType.LINK, commit);
        commit.createProperty("domainId", OType.STRING);
        commit.createProperty("connectorId", OType.STRING);
        commit.createProperty("instanceId", OType.STRING);
        commit.createProperty("comment", OType.STRING);
        commit.createProperty("revisionNumber", OType.STRING);
        commit.createProperty("parentRevisionNumber", OType.STRING);

        entity.createProperty("history", OType.LINK, history);

        history.createProperty("current", OType.LINK, entity);
        history.createProperty("first", OType.LINK, entity);
        history.createProperty("last", OType.LINK, entity);
        history.createProperty("revisions", OType.LINKLIST, revision);
        history.createProperty("archived", OType.BOOLEAN);

        revision.createProperty("commit",    OType.LINK, commit);
        revision.createProperty("history",   OType.LINK, history);
        revision.createProperty("prev",      OType.LINK, revision);
        revision.createProperty("next",      OType.LINK, revision);
        revision.createProperty("from",      OType.DATETIME);
        revision.createProperty("to",        OType.DATETIME);
    }

    public void addModel(Class<?> clazz) {
        models.add(clazz);
    }

    public void generateSchemaForModels() {
        List<OClass> modelClasses = new ArrayList<OClass>();
        List<Reference> references = new ArrayList<Reference>();

        // create classes and attributes for all models, references to other models are extracted and stored in a list
        for (Class<?> clazz : models) {
            modelClasses.add(createModelClass(clazz, references));
        }

        // create classes for history and revision vertices
        for (OClass modelClass : modelClasses) {
            createVersioning(modelClass);
        }

        // create classes for all edges
        createReferences(references);
    }

    private void createVersioning(OClass modelClass) {
        OClass modelHistory  = schema.createClass(modelClass.getName() + "History", history);
        OClass modelRevision = schema.createClass(modelClass.getName() + "Revision", revision);

        modelHistory.getProperty("current").set(OProperty.ATTRIBUTES.LINKEDCLASS, modelClass);
        modelHistory.getProperty("last").set(OProperty.ATTRIBUTES.LINKEDCLASS, modelRevision);
        modelHistory.getProperty("first").set(OProperty.ATTRIBUTES.LINKEDCLASS, modelRevision);
        modelHistory.getProperty("revisions").set(OProperty.ATTRIBUTES.LINKEDCLASS, modelRevision);

        modelRevision.getProperty("history").set(OProperty.ATTRIBUTES.LINKEDCLASS, modelHistory);
        modelRevision.getProperty("prev").set(OProperty.ATTRIBUTES.LINKEDCLASS, modelRevision);
        modelRevision.getProperty("next").set(OProperty.ATTRIBUTES.LINKEDCLASS, modelRevision);

        for (OProperty property : modelClass.properties()) {
            String name = property.getName();
            if (!name.equals("history")) {
                modelRevision.createProperty(name, property.getType(), property.getLinkedClass());
            }
        }

        modelClass.getName();
    }

    private OClass createModelClass(Class<?> clazz, List<Reference> references) {
        OClass modelClass;
        Class<?> superclass = clazz.getSuperclass();

        if (superclass == null) {
            modelClass = schema.createClass(clazz.getSimpleName(), entity);
        }
        else {
            modelClass = schema.createClass(clazz.getSimpleName(), schema.getClass(superclass.getSimpleName()));
        }

        createPropertiesForModel(clazz, modelClass, references);
        return modelClass;
    }

    private void createPropertiesForModel(Class<?> clazz, OClass modelClass, List<Reference> references) {
        for(Field field : clazz.getDeclaredFields()) {
            String propertyName = field.getName();
            OType propertyType = OType.getTypeByClass(field.getType());

            if (propertyType == null) {
                // not a native java type - so it should be a model
                // TODO add a check if it's really a model here
                // we create a reference (from, name, to) here to create the edge schema later
                // because the class for the referenced model might not be created here
                references.add(new Reference(modelClass, propertyName, field.getType()));
            }
            else if (propertyType == OType.EMBEDDEDLIST || propertyType == OType.EMBEDDEDSET) {
                // check if contents of lists/sets are native types or models
                Class<?> innerClass = (Class<?>)((ParameterizedType)field.getGenericType()).getActualTypeArguments()[0];
                OType innerPropertyType = OType.getTypeByClass(innerClass);

                if (innerPropertyType == null) {
                    // not a native java type - so it should be a model
                    // TODO add a check if it's really a model here
                    // lists or ets of models are handled by edges - so no properties are created
                    // we create a reference (from, name, to) here to create the edge schema later
                    // because the class for the referenced model might not be created here
                    references.add(new Reference(modelClass, propertyName, innerClass));
                }
                else {
                    // CREATE an EMBEDDEDLIST or EMBEDDEDSET as an property
                    modelClass.createProperty(propertyName, propertyType, innerPropertyType);
                }
            }
            else {
                // create a property for attributes other than RID
                if (!propertyName.equals("RID"))
                    modelClass.createProperty(propertyName, propertyType);
            }
        }
    }

    private void createReferences(List<Reference> references) {
        // create schema for edges
        // naming conflicts resolved by omitting type information on the edges
        // could also be resolved by using a naming convention for edge classes like: person_worksAt_project at the cost of
        // ugy queries;
        Set<String> edgeNames = new HashSet<String>();

        for (Reference reference : references) {
            boolean namingConflict = !edgeNames.add(reference.getName());
            if (namingConflict) {
                OClass ref = schema.getClass(reference.getName());
                // drop properties and recreating them to remove type contrains
                ref.dropProperty("out"); ref.createProperty("out", OType.LINK);
                ref.dropProperty("in"); ref.createProperty("in", OType.LINK);
            }
            else {
                OClass ref = schema.createClass(reference.getName(), E);
                ref.createProperty("out", OType.LINK, reference.getFrom());
                ref.createProperty("in", OType.LINK, schema.getClass(reference.getTo().getSimpleName()));
            }
        }
    }
}
