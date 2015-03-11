/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;

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

    public ODatabaseDocument getDatabase() {
        return database;
    }

    public void setDatabase(ODatabaseDocument database) {
        this.database = database;
    }

    public SchemaGenerator() {

    }

    public SchemaGenerator(ODatabaseDocument database) {
        setDatabase(database);
    }

    public void generateVersioningSchema() {
        schema = database.getMetadata().getSchema();

        V = schema.getOrCreateClass("V");
        E = schema.getOrCreateClass("E");
        revision = schema.createClass("Revision", V);
        commit = schema.createClass("Commit", V);
        relationship = schema.createClass("Relationship", V);
        history = schema.createAbstractClass("History", V);
        entity = schema.createAbstractClass("Entity", V);

        commit.createProperty("timestamp", OType.DATETIME);
        // replaced by inserts_eplan, inserts_opm...
        //commit.createProperty("inserts", OType.LINKLIST, history);
        //commit.createProperty("updates", OType.LINKLIST, revision);
        //commit.createProperty("deletes", OType.LINKLIST, history);
        commit.createProperty("insertedRelationships", OType.LINKLIST, relationship);
        commit.createProperty("deletedRelationships", OType.LINKLIST, relationship);
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

        createUIIDIndex();
    }

    private void createUIIDIndex() {
        database.command(new OCommandScript("sql", "create index uiid unique_hash_index String")).execute();
    }

    public void addModel(Class<?> clazz) {
        String modelName = clazz.getSimpleName();
        Class<?> superclass = clazz.getSuperclass();

        if (superclass == Object.class) {
            schema.createClass(modelName, entity);
            schema.createClass(modelName + "History", history);
        } else {
            schema.createClass(modelName, schema.getClass(superclass.getSimpleName()));
            schema.createClass(modelName + "History", schema.getClass(superclass.getSimpleName() + "History"));
        }
    }
}
