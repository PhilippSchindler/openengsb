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

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.intent.OIntent;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.query.OQuery;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.tx.OTransaction;
import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.core.api.model.OpenEngSBModelEntry;
import org.openengsb.core.ekb.api.Query;
import org.openengsb.core.ekb.api.TransformationDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

// TODO: implements EKBService
// EKBService need to be changed to the new EKBCommit Interface

public class EKBServiceOrientDB {

    private ODatabaseDocumentTx database;
    private OIndex<?> uiidIndex;

    // stores loaded current instances of documents required for a single commit
    // cleared at the start of each commit
    // so only a single database.load operation is needed per model for a commit
    // also allows for insert relationships between inserted nodes of the same commit (which get there RID after the
    // transaction is excecuted)
    private Map<OpenEngSBModel, ODocument> cachedDocuments;

    // if enabled, generated RIDs will be stored back into the models after a commit
    private boolean isRIDSupportEnabled = true;

    // if enabled, the current version of all models, the history of all deleted model
    // the current version of relationships and the revision of deleted relationships can be directly access by an index
    private boolean isUIIDIndexSupportEnabled = true;

    public boolean isRidSupportEnabled() {
        return isRIDSupportEnabled;
    }

    public void setRidSupportEnabled(boolean isRIDSupportEnabled) {
        this.isRIDSupportEnabled = isRIDSupportEnabled;
    }

    public boolean isUIIDIndexSupportEnabled() {
        return isUIIDIndexSupportEnabled;
    }

    public void setUIIDIndexSupportEnabled(boolean isUIIDIndexSupportEnabled) {
        this.isUIIDIndexSupportEnabled = isUIIDIndexSupportEnabled;
    }

    public ODatabaseDocumentTx getDatabase() {
        return database;
    }

    public void setDatabase(ODatabaseDocumentTx database) {
        this.database = database;
    }

    private ORID performInsertOperation(Operation operation, ODocument v_commit, List<ORID> insertedRIDs) {
        OpenEngSBModel model = operation.getModel();
        ODocument v_currentEntity = database.newInstance(getModelClassName(model));
        ODocument v_history = database.newInstance(getModelClassName(model) + "History");
        ODocument v_currentRevision = database.newInstance("Revision");

        // adding the newly created entity to the map of cachedDocuments, so we can use the document instance it in the
        // current commit for inserting relationships
        // cachedDocuments.put(model, v_currentEntity);            // TODO removed for descreasing memory footprint
                                                                   // may impact relationships !!!

        // copy the data from the model into the v_currentEntity
        convertModel(model, v_currentEntity);
        v_currentEntity.field("history", v_history);
        v_currentEntity.field("commit", v_commit);

        v_history.field("createdBy", v_commit);
        v_history.field("deletedBy", (ODocument) null);
        v_history.field("archived", false);
        v_history.field("current", v_currentEntity);
        v_history.field("last", v_currentRevision);
        v_history.field("first", v_currentRevision);
        List<ODocument> linkRevisions = new ArrayList<>();
        linkRevisions.add(v_currentRevision);
        v_history.field("revisions", linkRevisions);

        // copy the data from the model into the v_currentEntity
        copyDocumentData(v_currentEntity, v_currentRevision);
        v_currentRevision.field("commit", v_commit);
        v_currentRevision.field("history", v_history);

        v_currentEntity.field("uiid", model.retrieveInternalModelId());
        v_history.field("uiid", model.retrieveInternalModelId());

        // v_currentEntity.save();
        v_history.save();                   // saves connected records as well
        // v_currentRevision.save();

        addToIndex(model, v_currentEntity);

        insertedRIDs.add(v_history.getIdentity());
        return v_currentEntity.getIdentity();
    }

    private void performUpdateOperation(Operation operation, ODocument v_commit, List<ORID> updatedRIDs) {
        OpenEngSBModel model = operation.getModel();
        ODocument v_currentEntity = loadCurrentDocumentForModel(model);
        ODocument v_history = v_currentEntity.field("history");
        ODocument v_currentRevision = database.newInstance("Revision");
        ODocument v_previousRevision = v_history.field("last");

        // update current version (overwrite data)
        clearCurrentDataProperties(v_currentEntity);
        convertModel(model, v_currentEntity);
        v_currentEntity.field("commit", v_commit);

        // update the fields for the new revision (same data as the updated current revision)
        copyDocumentData(v_currentEntity, v_currentRevision);
        v_currentRevision.field("commit", v_commit);
        v_currentRevision.field("history", v_history);

        // if the current version has relationsships to other entities they need to be copied for the new revision
        for (Link link_entity_relationship : getLinks(v_currentEntity)) {
            ODocument r_current = link_entity_relationship.getTarget();
            ODocument r_revision = r_current.field("revision");
            // we have a link from the current version to the relationship node
            // the revision must have the same link to the revision node of the relationship
            createDataLink(v_currentRevision, link_entity_relationship.getName(), r_revision);

            for (Link link_relationship_entity : getLinks(r_current)) {
                if (link_relationship_entity.getTarget() == v_currentEntity) {
                    // link2 is a link from the current relationship back to the current entity
                    // this link must be copied for the new revision
                    // so we overwrite the current last revision with the new revision
                    r_revision.field(link_relationship_entity.getName(), v_currentRevision);

                    // and add the new revision to the list of all revisions
                    // TODO additional schema info is required here if the names are not in the form of
                    // e.g. person (to get recent revision) and personRevisions (to get all related revisions)
                    ((List<ODocument>) r_revision.field(link_relationship_entity.getName() + "Revisions"))
                            .add(v_currentRevision);
                }
            }
        }

        // update history
        ((List<ODocument>) v_history.field("revisions")).add(v_currentRevision);
        v_history.field("last", v_currentRevision);
        v_currentRevision.field("prev", v_previousRevision);
        v_previousRevision.field("next", v_currentRevision);

        // v_currentEntity.save();
        v_history.save();                   // saves connected vertices as well
        // v_currentRevision.save();
        // v_previousRevision.save();


        updatedRIDs.add(v_currentRevision.getIdentity());
    }

    private void performDeleteOperation(Operation operation, ODocument v_commit, List<ORID> deletedRIDs) {
        OpenEngSBModel model = operation.getModel();

        ODocument v_currentEntity = loadCurrentDocumentForModel(model);
        ODocument v_history = v_currentEntity.field("history");

        // update history
        v_history.field("archived", true);
        v_history.field("deletedBy", v_commit);
        v_history.field("current", (ODocument) null);
        v_history.save();

        // delete all relationships with the current_entity
        for (Link link_entity_relationship : getLinks(v_currentEntity)) {
            // target of link is a relationship vertex which should be removed
            ODocument r_current = link_entity_relationship.getTarget();
            ODocument r_revision = r_current.field("revision");

            // update the revision of this relationship - set deletedBy property to the current commit
            r_revision.field("deletedBy", v_commit);
            r_revision.removeField("current");
            r_revision.save();
            ((List<ODocument>) v_commit.field("deletedRelationships")).add(r_revision);

            // update nodes linked to this relationship - delete all links to this relationship vertex
            for (Link link_relationship_entity : getLinks(r_current)) {
                if (link_relationship_entity.getTarget() != v_currentEntity) {
                    ODocument v_relatedEntity = link_relationship_entity.getTarget();
                    deleteDataLink(v_relatedEntity, link_entity_relationship.getName(), r_current);
                }
            }

            // delete relationship node
            r_current.delete();
            updateIndex(r_current, r_revision);
        }

        // drop current version
        v_currentEntity.delete();
        updateIndex(model, v_history);

        deletedRIDs.add(v_history.getIdentity());
    }

    private ORID performRelationshipInsertOperation(Operation operation, ODocument v_commit) {
        OpenEngSBModel model = operation.getModel();
        Relationship relationship = (Relationship) model;

        ODocument r_current = database.newInstance("Relationship");
        ODocument r_revision = database.newInstance("Relationship");

        r_current.field("revision", r_revision);
        r_current.field("commit", v_commit);
        r_current.field("uiid", model.retrieveInternalModelId());
        r_revision.field("createdBy", v_commit);
        r_revision.field("current", r_current);
        r_revision.field("uiid", model.retrieveInternalModelId());

        for (OpenEngSBModel relatedModel : relationship.getRelatedModels()) {
            ODocument v_relatedEntity = loadCurrentDocumentForModel(relatedModel);
            ODocument v_relatedRevision = ((ODocument) v_relatedEntity.field("history")).field("last");

            // create link from the entity/revision to the relationship/relationshipRevision
            updateRelationshipProperty(v_relatedEntity, r_current, relationship);
            updateRelationshipProperty(v_relatedRevision, r_revision, relationship);

            // create link in the other direction - for the current revision only
            String linkName_current = relationship.getLinkNameForRecentRevision(relatedModel);
            r_current.field(linkName_current, v_relatedEntity);
            r_revision.field(linkName_current, v_relatedRevision);

            // create link in the other direction - for the all revisions
            String linkName_revisions = relationship.getLinkNameForAllRevisions(relatedModel);
            List<ODocument> l = new ArrayList<>();
            l.add(v_relatedRevision);
            r_revision.field(linkName_revisions, l);
        }

        // stores all changes recursively at the end of the transaction
        r_current.save();
        addToIndex(model, r_current);

        ((List<ODocument>) v_commit.field("insertedRelationships")).add(r_revision);
        return r_current.getIdentity();
    }

    private void performRelationshipDeleteOperation(Operation operation, ODocument v_commit) {
        OpenEngSBModel model = operation.getModel();
        Relationship relationship = (Relationship) model;

        ODocument r_current = loadCurrentDocumentForModel(model);
        ODocument r_revision = r_current.field("revision");

        for (String linkName : r_current.fieldNames()) {
            if (!linkName.equals("revision") && !linkName.equals("commit") && !linkName.equals("uiid")) {
                ODocument v_related = r_current.field(linkName);
                deleteDataLink(v_related, relationship.getName(), r_current);
                v_related.save();
            }
        }

        r_revision.field("deletedBy", v_commit);
        r_revision.removeField("current");
        r_revision.save();
        r_current.delete();

        updateIndex(model, r_revision);

        ((List<ODocument>) v_commit.field("deletedRelationships")).add(r_revision);
    }

    /**
     * creates a link from v_related to r_relationship
     * if there are already link with the same names a list for all links is created instead of a single field
     */
    private void updateRelationshipProperty(ODocument v_related, ODocument r_relationship, Relationship relationship) {

        Object p_linkToRelationship = v_related.field(relationship.getName());

        if (p_linkToRelationship == null) {
            v_related.field(relationship.getName(), r_relationship);
        } else {
            if (p_linkToRelationship instanceof List<?>) {
                ((List<ODocument>) p_linkToRelationship).add(r_relationship);
            } else {
                List<ODocument> links = new ArrayList<>();
                links.add((ODocument) p_linkToRelationship);
                links.add(r_relationship);
                v_related.field(relationship.getName(), links);
            }
        }
    }

    /**
     * creates or clear the list of cached documents
     * is called in the beginning of each commit to ensure we have the most recent version of the database
     */
    private void initCommit() {
        if (cachedDocuments == null) {
            cachedDocuments = new HashMap<>();
        }
        cachedDocuments.clear();

        if (isUIIDIndexSupportEnabled) {
            uiidIndex = database.getMetadata().getIndexManager().getIndex("uiid");
        }
    }

    private void storeGeneratedRIDsBack(EKBCommit commit, List<ORID> insertedModels,
            List<ORID> insertedRelationships) {
        if (isRIDSupportEnabled) {
            // store rid's back for future updates/deletes
            int i = 0;
            for (Operation operation : commit.getOperations(OperationType.INSERT)) {
                setRID(operation.getModel(), insertedModels.get(i));
                i++;
            }
            i = 0;
            for (Operation operation : commit.getOperations(OperationType.INSERT_RELATIONSHIP)) {
                setRID(operation.getModel(), insertedRelationships.get(i));
                i++;
            }
        }
    }

    /**
     * recursively converts an OpenEngSBModel to an ODocument which can be stored as vertex in OrientDB
     * if called the for the root model, argument document must be created via database.createInstance()
     * otherwise document == null, and a embedded document is created
     */
    public ODocument convertModel(OpenEngSBModel model, ODocument document) {
        if (document == null) {
            document = new ODocument();
        }
        for (OpenEngSBModelEntry entry : model.toOpenEngSBModelValues()) {
            // ignore empty properties or id's here
            if (entry.getValue() == null || entry.getKey().toUpperCase().equals("RID") ||
                    entry.getKey().toUpperCase().equals("UIID")) {
                continue;
            }
            if (entry.getType() == List.class) {
                document.field(entry.getKey(), convertList((List<?>) entry.getValue()), OType.EMBEDDEDLIST);
            } else if (entry.getType() == Set.class) {
                document.field(entry.getKey(), convertSet((Set<?>) entry.getValue()), OType.EMBEDDEDSET);
            } else if (entry.getType() == Map.class) {
                document.field(entry.getKey(), convertMap((Map<?, ?>) entry.getValue()), OType.EMBEDDEDMAP);
            } else if (entry.getValue() instanceof OpenEngSBModel) {
                document.field(entry.getKey(), convertModel((OpenEngSBModel) entry.getValue(), null), OType.EMBEDDED);
            } else {
                document.field(entry.getKey(), entry.getValue());
            }
        }
        return document;
    }

    public Object convert(Object object) {

        if (OType.isSimpleType(object)) {
            return object;
        }
        if (object instanceof OpenEngSBModel) {
            return convertModel((OpenEngSBModel) object, null);
        }
        if (object instanceof List<?>) {
            return convertList((List<?>) object);
        }
        if (object instanceof Set<?>) {
            return convertSet((Set<?>) object);
        }
        if (object instanceof Map<?, ?>) {
            return convertMap((Map<?, ?>) object);
        }

        throw new IllegalArgumentException("Cannot convert model to document. Object '"
                + object.toString() + "' failed!");
    }

    public List<Object> convertList(List<?> list) {
        List<Object> converted = new ArrayList<>();
        for (Object o : list) {
            converted.add(convert(o));
        }
        return converted;
    }

    public Set<Object> convertSet(Set<?> set) {
        Set<Object> converted = new HashSet<>();
        for (Object o : set) {
            converted.add(convert(o));
        }
        return converted;
    }

    public Map<String, Object> convertMap(Map<?, ?> map) {
        Map<String, Object> converted = new HashMap<>();
        for (Map.Entry<?, ?> kvp : map.entrySet()) {
            if (kvp.getKey() instanceof String) {
                converted.put((String) kvp.getKey(), convert(kvp.getValue()));
            } else {
                throw new IllegalArgumentException("Cannot convert model to document. Map key is not of type String!");
            }
        }
        return converted;
    }

    private ODocument loadCurrentDocumentForModel(OpenEngSBModel model) {
        // check if this model was already accessed in the current commit
        // if yes return the known instance
        ODocument cachedDocument = cachedDocuments.get(model);
        if (cachedDocument != null)
            return cachedDocument;

        // if the model has an RID, fast load it by it's RID
        ORID rid = getRID(model);
        if (rid != null) {
            ODocument loadedDocument = database.load(rid);
            // cachedDocuments.put(model, loadedDocument);              // TODO check (see other comment)
            return loadedDocument;
        }

        // otherwise load it buy it's UIID (hash index-lookup), slower should still be O(1)
        if (isUIIDIndexSupportEnabled) {
            return database.load((ORecordId) uiidIndex.get(model.retrieveInternalModelId().toString()));
        }

        // not implemented
        throw new IllegalArgumentException("Unable to load the model. No RID/UIID available!");
    }

    private void copyDocumentData(ODocument from, ODocument to) {
        for (String fieldName : from.fieldNames()) {
            Object field = from.field(fieldName);
            // copy any field except links
            if (!(field instanceof ODocument) || ((ODocument) field).isEmbedded()) {
                to.field(fieldName, field);
            }
        }
    }

    private ODocument createCommitVertex(EKBCommit commit, Date timestamp, ODocument v_parentCommit) {
        ODocument v_commit = database.newInstance("Commit");

        v_commit.field("timestamp", timestamp);
        v_commit.field("domainId", commit.getDomainId());
        v_commit.field("connectorId", commit.getConnectorId());
        v_commit.field("instanceId", commit.getInstanceId());
        v_commit.field("comment", commit.getComment());
        v_commit.field("revisionNumber", commit.getRevisionNumber());

        if (v_parentCommit != null) {
            v_commit.field("parentRevisionNumber", v_parentCommit.field("revisionNumber"));
            v_commit.field("parent", v_parentCommit);
            v_parentCommit.field("next", v_commit);
        }

        v_commit.field("insertedRelationships", new ArrayList<ORID>());
        v_commit.field("deletedRelationships", new ArrayList<ORID>());

        return v_commit;
    }

    private void createDataLink(ODocument linkStorage, String linkName, ODocument linkTarget) {
        if (isLink(linkStorage, linkName)) {
            // link with this name already exists
            Object existing = linkStorage.field(linkName);
            if (existing instanceof ODocument) {
                // there is just one link already stored - so we need to create a list for this one and the new one
                List<ODocument> links = new ArrayList<>();
                links.add((ODocument) existing);
                links.add(linkTarget);
            } else {
                // there are at least two links already stored, so we just add the new one to the list
                ((List<ODocument>) existing).add(linkTarget);
            }
        } else {
            // create the new first link as property
            linkStorage.field(linkName, linkTarget);
        }
    }

    private void deleteDataLink(ODocument linkStorage, String linkName, ODocument linkTarget) {
        Object linking = linkStorage.field(linkName);
        if (linking instanceof ODocument) {
            linkStorage.removeField(linkName);
        } else {
            List<ODocument> links = (List<ODocument>) linking;
            links.remove(linkTarget);
            if (links.size() == 1)
                linkStorage.field(linkName, links.get(0));
        }
    }

    private void clearCurrentDataProperties(ODocument doc) {
        for (String fieldName : doc.fieldNames()) {
            if (!isInternalLinkName(fieldName) && !isLink(doc, fieldName)) {
                doc.removeField(fieldName);
            }
        }
    }

    private String getModelClassName(OpenEngSBModel model) {
        return model.getClass().getSimpleName();
    }

    private boolean isLink(ODocument doc, String fieldName) {
        Object field = doc.field(fieldName);
        if (field instanceof ODocument) {
            return !((ODocument) field).isEmbedded();
        }
        if (field instanceof List<?>) {
            for (Object target : (List<?>) field) {
                if (target instanceof ODocument)
                    return !((ODocument) target).isEmbedded();
                return false;
            }
        }
        return false;
    }

    private boolean isInternalLinkName(String fieldName) {
        return fieldName.equals("history") || fieldName.equals("commit") || fieldName.equals("revision");
    }

    private List<Link> getLinks(ODocument doc) {
        List<Link> links = new ArrayList<>();
        for (String fieldName : doc.fieldNames()) {
            if (!isInternalLinkName(fieldName)) {
                Object field = doc.field(fieldName);
                if (field instanceof ODocument) {
                    links.add(new Link(fieldName, (ODocument) doc.field(fieldName)));
                } else if (field instanceof List<?>) {
                    for (Object target : (List<?>) field) {
                        if (!(target instanceof ODocument)) {
                            break;
                        }
                        links.add(new Link(fieldName, (ODocument) target));
                    }
                }
            }
        }
        return links;
    }

    private ORID getRID(OpenEngSBModel model) {
        try {
            return new ORecordId((String) model.getClass().getMethod("getRID").invoke(model));
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setRID(OpenEngSBModel model, ORID rid) {
        try {
            model.getClass().getMethod("setRID", String.class).invoke(model, rid.toString());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void addToIndex(OpenEngSBModel key, ODocument value) {
        if (isUIIDIndexSupportEnabled) {
            uiidIndex.put(key.retrieveInternalModelId().toString(), value);
        }
    }

    private void updateIndex(OpenEngSBModel key, ODocument value) {
        if (isUIIDIndexSupportEnabled) {
            uiidIndex.remove(key.retrieveInternalModelId().toString());
            uiidIndex.put(key.retrieveInternalModelId().toString(), value);
        }
    }

    private void updateIndex(ODocument keyDoc, ODocument value) {
        if (isUIIDIndexSupportEnabled) {
            String key = keyDoc.field("uiid");
            uiidIndex.remove(key);
            uiidIndex.put(key, value);
        }
    }

    //@Override
    public void commit(EKBCommit ekbCommit) {
        //database.declareIntent(new OIntentMassiveInsert());
        Date timestamp = new Date();
        ODocument v_commit = createCommitVertex(ekbCommit, timestamp, null);

        List<ORID> v_inserted = new ArrayList<>();
        List<ORID> v_insertedRelationships = new ArrayList<>();
        List<Operation> insertOperations = ekbCommit.getOperations(OperationType.INSERT);
        List<Operation> insertRelationshipOperations = ekbCommit.getOperations(OperationType.INSERT_RELATIONSHIP);

        List<ORID> insertedRIDs = new ArrayList<>();
        List<ORID> updatedRIDs = new ArrayList<>();
        List<ORID> deletedRIDs = new ArrayList<>();

        initCommit();

        for (Operation operation : ekbCommit.getOperations(OperationType.DELETE_RELATIONSHIP)) {
            performRelationshipDeleteOperation(operation, v_commit);
        }

        for (Operation operation : ekbCommit.getOperations(OperationType.DELETE)) {
            performDeleteOperation(operation, v_commit, deletedRIDs);
        }

        for (Operation operation : ekbCommit.getOperations(OperationType.UPDATE)) {
            performUpdateOperation(operation, v_commit, updatedRIDs);
        }

        for (Operation operation : insertOperations) {
            v_inserted.add(performInsertOperation(operation, v_commit, insertedRIDs));
        }

        for (Operation operation : insertRelationshipOperations) {
            v_insertedRelationships.add(performRelationshipInsertOperation(operation, v_commit));
        }


        v_commit.field("inserts", insertedRIDs);
        v_commit.field("updates", updatedRIDs);
        v_commit.field("deletes", deletedRIDs);
        v_commit.save();



        database.commit();

        storeGeneratedRIDsBack(ekbCommit, v_inserted, v_insertedRelationships);
    }

    //@Override
    public void commit(EKBCommit ekbCommit, UUID headRevision) {
        // TODO
        throw new UnsupportedOperationException();
    }

    //@Override
    public void addTransformation(TransformationDescriptor descriptor) {
        throw new UnsupportedOperationException();
    }

    //@Override
    public <T> List<T> query(Query query) {
        throw new UnsupportedOperationException();
    }

    //@Override
    public Object nativeQuery(Object query) {
        List<ODocument> result;
        if (query instanceof String) {
            result = database.query(new OSQLSynchQuery<ODocument>((String) query));
        } else if (query instanceof OQuery<?>) {
            result = database.query((OQuery<?>) query);
        } else {
            throw new IllegalArgumentException("Invalid argument query! (use a String or a OQuery instance)");
        }
        return result;
    }

    //@Override
    public UUID getLastRevisionId() {
        // TODO
        throw new UnsupportedOperationException();
    }

    // @Override
    public void deleteCommit(UUID headRevision) {
        throw new UnsupportedOperationException();
    }

    // @Override
    public EKBCommit loadCommit(UUID revision) {
        throw new UnsupportedOperationException();
    }

    // @Override
    public <T> T getModel(Class<T> model, String oid) {
        // TODO what's oid?
        throw new UnsupportedOperationException();
    }
}
