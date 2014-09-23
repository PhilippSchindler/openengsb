package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.query.OQuery;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.core.api.model.OpenEngSBModelEntry;
import org.openengsb.core.ekb.api.Query;
import org.openengsb.core.ekb.api.TransformationDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */
// TODO: implements EKBService
// EKBService need to be changed to the new EKBCommit Interface

public class EKBServiceOrientDB {

    private ODatabaseDocumentTx database;

    // stores loaded current instances of documents required for a single commit
    // cleared at the start of each commit
    // so only a single database.load operation is needed per model for a commit
    // also allows for insert relationships between inserted nodes of the same commit (which get there RID after the
    // transaction is excecuted)
    private Map<OpenEngSBModel, ODocument> cachedDocuments;

    // if enabled, generated RIDs will be stored back into the models after a commit
    private boolean isRIDSupportEnabled = true;



    public ODatabaseDocumentTx getDatabase() {
        return database;
    }

    public void setDatabase(ODatabaseDocumentTx database) {
        this.database = database;
    }

    public boolean isRidSupportEnabled() {
        return isRIDSupportEnabled;
    }

    public void setRidSupportEnabled(boolean isRIDSupportEnabled) {
        this.isRIDSupportEnabled = isRIDSupportEnabled;
    }



    private ODocument performInsertOperation(Operation operation, ODocument v_commit) {
        OpenEngSBModel model = operation.getModel();

        ODocument v_entity = database.newInstance(getModelClassName(model));
        cachedDocuments.put(model, v_entity);
        ODocument v_history = database.newInstance(getModelClassName(model) + "History");
        ODocument v_revision = database.newInstance("Revision");

        Map<String, Object> properties = extractProperties(model);

        v_entity.fields(properties);
        v_entity.field("history", v_history);
        v_entity.field("commit", v_commit);

        v_history.field("createdBy", v_commit);
        v_history.field("deleteBy", (ODocument) null);
        v_history.field("archived", false);
        v_history.field("current", v_entity);
        v_history.field("last", v_revision);
        v_history.field("first", v_revision);
        List<ODocument> linkRevisions = new ArrayList<>();
        linkRevisions.add(v_revision);
        v_history.field("revisions", linkRevisions);


        v_revision.fields(properties);
        v_revision.field("commit", v_commit);
        v_revision.field("history", v_history);

        v_history.save();
        v_entity.save();
        v_revision.save();

        return v_entity;
    }

    private void performUpdateOperation(Operation operation, ODocument v_commit) {
        OpenEngSBModel model = operation.getModel();

        ODocument v_entity = loadCurrentDocumentForModel(model);
        ODocument v_revision = database.newInstance("Revision");
        ODocument v_history  = v_entity.field("history");
        ODocument v_prev_revision = v_history.field("last");

        // update the fields for the new current version
        clearCurrentDataProperties(v_entity);
        v_entity.fields(extractProperties(model));
        v_entity.field("commit", v_commit);

        // update the fields for the new revision
        v_revision.fields(extractProperties(model));
        v_revision.field("commit", v_commit);
        v_revision.field("history", v_history);

        // if there are relationships with v_entity, the links from the relationship to the new revision must be created
        for (Link link : getLinks(v_entity)) {
            ODocument e_current = link.getTarget();
            ODocument e_revision = e_current.field("revision");

            // we have a like from the current version to the relationship node
            // the revision must have the same link to the revision node of the relationship
            createDataLink(v_revision, link.getName(), e_revision);

            for (Link link2 : getLinks(e_current)) {
                if (link2.getTarget() == v_entity) {
                    // this is a link from the current relationship back to the current entity
                    // this link must be copied for the new revision
                    // so we overwrite the current last revision with the new revision
                    e_revision.field(link2.getName(), v_revision);

                    // and add the new revision to the list of all revisions
                    // TODO additional schema info is required here if the names are not in the form of
                    // e.g. person (to get recent revision) and personRevisions (to get all related revisions)
                    ((List<ODocument>)e_revision.field(link2.getName() + "Revisions")).add(v_revision);
                }
            }
        }

        // update history
        ((List<ODocument>) v_history.field("revisions")).add(v_revision);
        v_history.field("last", v_revision);
        v_revision.field("prev", v_prev_revision);
        v_prev_revision.field("next", v_revision);

        v_entity.save();
        v_history.save();
        v_revision.save();
        v_prev_revision.save();
    }

    private void performDeleteOperation(Operation operation, ODocument v_commit) {
        OpenEngSBModel model = operation.getModel();

        ODocument v_entity = loadCurrentDocumentForModel(model);
        ODocument v_history  = v_entity.field("history");

        // update history
        v_history.field("archived", true);
        v_history.field("deleteBy", v_commit);
        v_history.field("current", (ODocument)null);
        v_history.save();

        for (Link v_entity_link : getLinks(v_entity)) {
            // target of link is a relationship vertex which should be removed

            // update lastRevision of this relationship - set deletedBy property to this commit
            ODocument e_lastRevision = v_entity_link.getTarget().field("revision");
            e_lastRevision.field("deleteBy", v_commit);
            e_lastRevision.save();

            ODocument e_current = v_entity_link.getTarget();

            // update nodes linked to this relationship - delete all links to this relationship vertex
            for (Link e_current_link : getLinks(e_current)) {
                if (e_current_link.getTarget() !=  v_entity) {
                    // the target vertex is not the one we want to delete so just the links are removed
                    ODocument v_related = e_current_link.getTarget();
                    deleteDataLink(v_related, v_entity_link.getName(), e_current);
                }
            }

            // delete relationship node
            e_current.delete();
        }

        // drop current version
        v_entity.delete();
    }

    private ODocument performRelationshipInsertOperation(Operation operation, ODocument v_commit) {
        OpenEngSBModel model = operation.getModel();
        Relationship relationship = (Relationship) model;

        ODocument e_current = database.newInstance("Relationship");
        ODocument e_revision = database.newInstance("Relationship");

        e_current.field("revision", e_revision);
        e_current.field("commit", v_commit);
        e_revision.field("createdBy", v_commit);

        for (OpenEngSBModel relatedModel : relationship.getRelatedModels()) {
            ODocument v_related_current = loadCurrentDocumentForModel(relatedModel);
            ODocument v_related_lastRevision = ((ODocument)v_related_current.field("history")).field("last");
            updateRelationshipProperty(v_related_current, e_current, relatedModel, relationship);
            updateRelationshipProperty(v_related_lastRevision, e_revision, relatedModel, relationship);

            String linkName_current = relationship.getLinkNameForRecentRevision(relatedModel);
            String linkName_revisions = relationship.getLinkNameForAllRevisions(relatedModel);

            e_current.field(linkName_current, v_related_current);
            e_revision.field(linkName_current, v_related_lastRevision);

            List<ODocument> l = new ArrayList<>();
            l.add(v_related_lastRevision);
            e_revision.field(linkName_revisions, l);
        }

        // stores all changes recursively at the end of the transaction
        e_current.save();

        return e_current;
    }

    private void performRelationshipDeleteOperation(Operation operation, ODocument v_commit) {
        OpenEngSBModel model = operation.getModel();
        Relationship relationship = (Relationship) model;

        ODocument e_current = loadCurrentDocumentForModel(model);
        ODocument e_revision = e_current.field("revision");

        for (String linkName : e_current.fieldNames()) {
            if (!linkName.equals("revision") && !linkName.equals("commit")) {
                ODocument v_related = e_current.field(linkName);
                deleteDataLink(v_related, relationship.getName(), e_current);
                v_related.save();
            }
        }

        e_revision.field("deletedBy", v_commit);
        e_revision.save();
        e_current.delete();
    }

    private void updateRelationshipProperty(ODocument v_related, ODocument e_relationship, OpenEngSBModel relatedModel,
                                            Relationship relationship) {

        Object p_linkToRelationship = v_related.field(relationship.getName());

        if (p_linkToRelationship == null) {
            v_related.field(relationship.getName(), e_relationship);
        }
        else {
            if (p_linkToRelationship instanceof List<?>) {
                ((List<ODocument>) p_linkToRelationship).add(e_relationship);
            }
            else {
                List<ODocument> links = new ArrayList<>();
                links.add((ODocument)p_linkToRelationship);
                links.add(e_relationship);
                v_related.field(relationship.getName(), links);
            }
        }
    }



    private void initDocumentCache() {
        if (cachedDocuments == null) {
            cachedDocuments = new HashMap<>();
        }
        cachedDocuments.clear();
    }

    private void storeGeneratedRIDsBack(EKBCommit commit, List<ODocument> insertedModels,
                                        List<ODocument> insertedRelationships) {
        if (isRIDSupportEnabled) {
            // store rid's back for future updates/deletes
            int i = 0;
            for (Operation operation : commit.getOperations(OperationType.INSERT)) {
                setRID(operation.getModel(), insertedModels.get(i).getIdentity());
                i++;
            }
            i = 0;
            for (Operation operation : commit.getOperations(OperationType.INSERT_RELATIONSHIP)) {
                setRID(operation.getModel(), insertedRelationships.get(i).getIdentity());
                i++;
            }
        }
    }

    private Map<String, Object> extractProperties(OpenEngSBModel model) {
        Map<String, Object> properties = new HashMap<>();
        for (OpenEngSBModelEntry entry : model.toOpenEngSBModelValues()) {
            if (entry.getValue() != null && !entry.getKey().equals("RID")) {
                if (OType.getTypeByClass(entry.getType()) == null) {
                    throw new IllegalArgumentException("Invalid model - some properties of this model cannot be " +
                            "persisted!");
                }
                else {
                    properties.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return properties;
    }


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
            }
            else if (entry.getType() == Set.class) {
                document.field(entry.getKey(), convertSet((Set<?>)entry.getValue()), OType.EMBEDDEDSET);
            }
            else if (entry.getType() == Map.class) {
                document.field(entry.getKey(), convertMap((Map<?,?>)entry.getValue()), OType.EMBEDDEDMAP);
            }
            else if (entry.getValue() instanceof OpenEngSBModel) {
                document.field(entry.getKey(), convertModel((OpenEngSBModel)entry.getValue(), null), OType.EMBEDDED);
            }
            else {
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
            return convertModel((OpenEngSBModel)object, null);
        }
        if (object instanceof List<?>) {
            return convertList((List<?>)object);
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
            cachedDocuments.put(model, loadedDocument);
            return loadedDocument;
        }

        // otherwise load it buy it's UIID (hash index-lookup), slower should still be O(1)
        // not implemented
        throw new IllegalArgumentException("Unable to load the model. No RID available!");
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

        return v_commit;
    }

    private void createDataLink(ODocument linkStorage, String linkName, ODocument linkTarget) {
        if (isLink(linkStorage, linkName)) {
            // link with this name already exists
            Object existing = linkStorage.field(linkName);
            if (existing instanceof ODocument) {
                // there is just one link already stored - so we need to create a list for this one and the new one
                List<ODocument> links = new ArrayList<>();
                links.add((ODocument)existing);
                links.add(linkTarget);
            }
            else {
                // there are at least two links already stored, so we just add the new one to the list
                ((List<ODocument>)existing).add(linkTarget);
            }
        }
        else {
            // create the new first link as property
            linkStorage.field(linkName, linkTarget);
        }
    }

    private void deleteDataLink(ODocument linkStorage, String linkName, ODocument linkTarget) {
        Object linking = linkStorage.field(linkName);
        if (linking instanceof ODocument) {
            linkStorage.removeField(linkName);
        }
        else {
            List<ODocument> links = (List<ODocument>)linking;
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
                }
                else if (field instanceof List<?>) {
                    for (Object target : (List<?>) field) {
                        if (!(target instanceof ODocument))
                            break;
                        links.add(new Link(fieldName, (ODocument) target));
                    }
                }
            }
        }
        return links;
    }

    private ORID getRID(OpenEngSBModel model) {
        List<OpenEngSBModelEntry> entries = model.toOpenEngSBModelValues();
        for (OpenEngSBModelEntry entry : entries) {
            if (entry.getKey().equals("RID")) {
                if (entry.getValue() == null) {
                    return null;
                }
                return new ORecordId((String) entry.getValue());
            }
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





    //@Override
    public void commit(EKBCommit ekbCommit) {

        Date timestamp = new Date();
        ODocument v_commit = createCommitVertex(ekbCommit, timestamp, null);

        List<ODocument> v_inserted = new ArrayList<>();
        List<ODocument> v_insertedRelationships = new ArrayList<>();
        List<Operation> insertOperations = ekbCommit.getOperations(OperationType.INSERT);
        List<Operation> insertRelationshipOperations = ekbCommit.getOperations(OperationType.INSERT_RELATIONSHIP);

        initDocumentCache();

        for (Operation operation : ekbCommit.getOperations(OperationType.DELETE_RELATIONSHIP)) {
            performRelationshipDeleteOperation(operation, v_commit);
        }

        for (Operation operation : ekbCommit.getOperations(OperationType.DELETE)) {
            performDeleteOperation(operation, v_commit);
        }

        for (Operation operation : ekbCommit.getOperations(OperationType.UPDATE)) {
            performUpdateOperation(operation, v_commit);
        }

        for (Operation operation : insertOperations) {
            v_inserted.add(performInsertOperation(operation, v_commit));
        }

        for (Operation operation : insertRelationshipOperations) {
            v_insertedRelationships.add(performRelationshipInsertOperation(operation, v_commit));
        }

        v_commit.save();
        database.commit();

        storeGeneratedRIDsBack(ekbCommit, v_inserted, v_insertedRelationships);
    }

    //@Override
    public void commit(EKBCommit ekbCommit, UUID headRevision) {
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
            result = database.query(new OSQLSynchQuery<ODocument>((String)query));
        }
        else if (query instanceof OQuery<?>) {
            result = database.query((OQuery<?>)query);
        }
        else {
            throw new IllegalArgumentException("Invalid argument query! (use a String or a OQuery instance)");
        }
        return result;
    }

    //@Override
    public UUID getLastRevisionId() {
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
        throw new UnsupportedOperationException();
    }
}