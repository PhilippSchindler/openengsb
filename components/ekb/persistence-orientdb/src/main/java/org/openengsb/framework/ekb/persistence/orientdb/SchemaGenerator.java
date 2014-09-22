package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */
public class SchemaGenerator {

    private ODatabaseDocument database;
    private OSchema schema;

    private OClass V;
    private OClass E;
    private OClass entity;
    private OClass commit;
    private OClass history;
    private OClass revision;
    private OClass relationship;


    public SchemaGenerator() {

    }

    public SchemaGenerator(ODatabaseDocument database) {
        setDatabase(database);
    }

    public ODatabaseDocument getDatabase() {
        return database;
    }

    public void setDatabase(ODatabaseDocument database) {
        this.database = database;
    }

    public void generateVersioningSchema()
    {
        schema = database.getMetadata().getSchema();

        V            = schema.getClass("V");
        E            = schema.getClass("E");
        revision     = schema.createClass("Revision", V);
        commit       = schema.createClass("Commit", V);
        relationship = schema.createClass("Relationship", V);
        history      = schema.createAbstractClass("History", V);
        entity       = schema.createAbstractClass("Entity", V);

        commit.createProperty("timestamp", OType.DATETIME);
        commit.createProperty("inserts", OType.LINKLIST, revision);
        commit.createProperty("updates", OType.LINKLIST, revision);
        commit.createProperty("deletes", OType.LINKLIST, revision);
        commit.createProperty("insertedRelationships", OType.LINKLIST, V);
        commit.createProperty("deletedRelationships", OType.LINKLIST, V);
        commit.createProperty("parent", OType.LINK, commit);
        commit.createProperty("next", OType.LINK, commit);
        commit.createProperty("domainId", OType.STRING);
        commit.createProperty("connectorId", OType.STRING);
        commit.createProperty("instanceId", OType.STRING);
        commit.createProperty("comment", OType.STRING);
        commit.createProperty("revisionNumber", OType.STRING);
        commit.createProperty("parentRevisionNumber", OType.STRING);

        history.createProperty("archived", OType.BOOLEAN);
        history.createProperty("current", OType.LINK, entity);
        history.createProperty("last", OType.LINK, revision);
        history.createProperty("first", OType.LINK, revision);
        history.createProperty("revisions", OType.LINKLIST, revision);
        history.createProperty("createdBy", OType.LINK, commit);
        history.createProperty("deletedBy", OType.LINK, commit);

        revision.createProperty("next", OType.LINK, revision);
        revision.createProperty("prev", OType.LINK, revision);
        revision.createProperty("commit", OType.LINK, commit);
        revision.createProperty("history", OType.LINK, history);

        entity.createProperty("commit", OType.LINK, commit);
        entity.createProperty("history", OType.LINK, history);

        relationship.createProperty("createdBy", OType.LINK, commit);
        relationship.createProperty("deletedBy", OType.LINK, commit);
    }

    public void addModel(Class<?> clazz) {
        String modelName = clazz.getSimpleName();
        Class<?> superclass = clazz.getSuperclass();

        if (superclass == Object.class) {
            schema.createClass(modelName, entity);
            schema.createClass(modelName + "History", history);
        }
        else {
            schema.createClass(modelName, schema.getClass(superclass.getSimpleName()));
            schema.createClass(modelName + "History", schema.getClass(superclass.getSimpleName() + "History"));
        }
    }
}
