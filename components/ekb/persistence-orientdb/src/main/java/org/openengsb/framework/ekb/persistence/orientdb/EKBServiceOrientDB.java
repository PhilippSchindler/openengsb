package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
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

    OrientGraph graph;
    ODatabaseDocumentTx database;

    public OrientGraph getDatabase() {
        return graph;
    }

    public void setDatabase(OrientGraph database) {
        this.graph = database;
        this.database = graph.getRawGraph();
    }

    //@Override
    public void commit(EKBCommit ekbCommit) {

        Date timestamp = new Date();
        ODocument v_commit = createCommitVertex(ekbCommit, timestamp, null);

        List<ODocument> v_inserted = new ArrayList<>();
        List<Operation> insertOperations = ekbCommit.getOperations(OperationType.INSERT);

        for (Operation operation : insertOperations) {
            v_inserted.add(performInsertOperation(operation, v_commit));
        }

        for (Operation operation : ekbCommit.getOperations(OperationType.UPDATE)) {
            performUpdateOperation(operation, v_commit);
        }

        for (Operation operation : ekbCommit.getOperations(OperationType.DELETE)) {
            performDeleteOperation(operation, v_commit);
        }




        //OpenEngSBModel insert = inserts.get(0);

        //Map<String, Object> properties = new HashMap<String, Object>();
        //Map<String, List<OpenEngSBModel>> references = new HashMap<String,  List<OpenEngSBModel>>();
        //extractProperties(insert, properties, references);

//        List<OpenEngSBModelEntry> entries = insert.toOpenEngSBModelValues();
//        OpenEngSBModelEntry fullname     = entries.get(2);
//        OpenEngSBModelEntry phoneNumbers = entries.get(5);
//        OpenEngSBModelEntry performs     = entries.get(6);

        v_commit.save();

        database.commit();


        // store rid's back for future updates/deletes
        int i = 0;
        for (Operation operation : insertOperations) {
            setRID(operation.getModel(), v_inserted.get(i).getIdentity());
            i++;
        }
    }


    private ODocument performInsertOperation(Operation operation, ODocument v_commit) {
        OpenEngSBModel model = operation.getModel();

        ODocument v_entity = database.newInstance(getModelClassName(model));
        ODocument v_history = database.newInstance(getModelClassName(model) + "History");
        ODocument v_revision = database.newInstance("Revision");

        Map<String, Object> properties = extractProperties(model);

        v_entity.fields(properties);
        v_entity.field("history", v_history);
        v_entity.field("commit", v_commit);

        v_history.field("createdBy", v_commit);
        v_history.field("deleteBy", (ODocument) null);
        v_history.field("archived", false);
        v_history.field("revisions", Arrays.asList(v_revision));
        v_history.field("current", v_entity);
        v_history.field("last", v_revision);
        v_history.field("first", v_revision);

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

        ODocument v_entity = database.load(getRID(model));
        ODocument v_revision = database.newInstance("Revision");
        ODocument v_history  = v_entity.field("history");
        ODocument v_prev_revision = v_history.field("last");

        // update the fields for the new current version
        v_entity.clear();
        v_entity.fields(extractProperties(model));
        v_entity.field("commit", v_commit);
        v_entity.field("history", v_history);

        // update the fields for the new revision
        v_revision.fields(extractProperties(model));
        v_revision.field("commit", v_commit);
        v_revision.field("history", v_history);

        // update history
        ((List<ODocument>) v_history.field("revisions")).add(v_revision);
        v_history.field("last", v_revision);
        v_revision.field("prev", v_prev_revision);
        v_prev_revision.field("next", v_revision);

        v_entity.save();
        v_history.save();
        v_revision.save();
        v_prev_revision.save();

        // TODO check if relationships should be inherited here and create/update them
    }

    private void performDeleteOperation(Operation operation, ODocument v_commit) {
        OpenEngSBModel model = operation.getModel();

        ODocument v_entity = database.load(getRID(model));
        ODocument v_history  = v_entity.field("history");

        // update history
        v_history.field("archived", true);
        v_history.field("deleteBy", v_commit);
        v_history.field("current", (ODocument)null);

        // drop current version
        v_entity.delete();

        v_history.save();
        // TODO handle relationships
    }




    private String getModelClassName(OpenEngSBModel model) {
        return model.getClass().getSimpleName();
    }

    private ORID getRID(OpenEngSBModel model) {
        List<OpenEngSBModelEntry> entries = model.toOpenEngSBModelValues();
        for (OpenEngSBModelEntry entry : entries) {
            if (entry.getKey().equals("RID")) {
                if (entry.getValue() == null) {
                    throw new IllegalArgumentException("RID of model not set. RID is required for an update!");
                }
                return new ORecordId((String) entry.getValue());
            }
        }
        throw new IllegalArgumentException("RID of model not set. RID is required for an update!");
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








    private void insertModel(OpenEngSBModel model, OrientVertex v_commit)
    {
        String modelClassName = model.getClass().getSimpleName();
        OrientVertex v_current = graph.addVertex("class:" + modelClassName);
        OrientVertex v_history = graph.addVertex("class:" + modelClassName + "History");
        OrientVertex v_revision = graph.addVertex("class:" + modelClassName + "Revision");

        v_current.setProperty("history", v_history);
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


    private void extractProperties(OpenEngSBModel model, Map<String, Object> properties,
                                   Map<String, List<OpenEngSBModel>> references) {

        Relationship[] relationships = model.getClass().getAnnotationsByType(Relationship.class);


        for (OpenEngSBModelEntry entry : model.toOpenEngSBModelValues()) {
            if (entry.getValue() != null) {
                // ignore null values

                boolean isRelationShip = false;
                try {
                    isRelationShip = model.getClass().getDeclaredField(entry.getKey()).isAnnotationPresent(Relationship.class);
                } catch (NoSuchFieldException e) {
                    // this should really never happen :) TODO: probably a better way to check this?
                    e.printStackTrace();
                }

                if (isRelationShip) {
                    if (entry.getValue() instanceof OpenEngSBModel) {
                        // we have a single reference to an other model here
                        references.put(entry.getKey(), Arrays.asList((OpenEngSBModel[]) entry.getValue()));
                    } else {
                        // we have multiple references to other models
                        references.put(entry.getKey(), (List<OpenEngSBModel>) entry.getValue());
                    }
                } else if (OType.getTypeByClass(entry.getType()) != null) {
                    properties.put(entry.getKey(), entry.getValue());
                } else {
                    // TODO throw error
                    // Model violated contraints, properties of this datatype cannot be stored in OrientDB
                }
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

        return v_commit;
    }








































    //@Override
    public void commit(EKBCommit ekbCommit, UUID headRevision) {

    }

    //@Override
    public void addTransformation(TransformationDescriptor descriptor) {

    }

    //@Override
    public <T> List<T> query(Query query) {
        return null;
    }

    //@Override
    public Object nativeQuery(Object query) {
        return null;
    }

    //@Override
    public UUID getLastRevisionId() {
        return null;
    }

    // @Override
    public void deleteCommit(UUID headRevision) {

    }

    // @Override
    public EKBCommit loadCommit(UUID revision) {
        return null;
    }

    // @Override
    public <T> T getModel(Class<T> model, String oid) {
        return null;
    }
}