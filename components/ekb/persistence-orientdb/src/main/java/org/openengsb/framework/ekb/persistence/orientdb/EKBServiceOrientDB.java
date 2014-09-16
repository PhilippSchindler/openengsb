package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.core.api.model.OpenEngSBModelEntry;
import org.openengsb.core.ekb.api.EKBCommit;
import org.openengsb.core.ekb.api.EKBService;
import org.openengsb.core.ekb.api.Query;
import org.openengsb.core.ekb.api.TransformationDescriptor;

import java.util.*;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */
public class EKBServiceOrientDB implements EKBService {

    OrientGraph graph;
    ODatabaseDocumentTx database;

    public OrientGraph getDatabase() {
        return graph;
    }

    public void setDatabase(OrientGraph database) {
        this.graph = database;
        this.database = graph.getRawGraph();
    }

    @Override
    public void commit(EKBCommit ekbCommit) {

        Date timestamp = new Date();
        OrientVertex v_commit = createCommitVertex(ekbCommit, timestamp, null);



        List<OpenEngSBModel> inserts = ekbCommit.getInserts();
        OpenEngSBModel insert = inserts.get(0);

        Map<String, Object> properties = new HashMap<String, Object>();
        Map<String, List<OpenEngSBModel>> references = new HashMap<String,  List<OpenEngSBModel>>();
        extractProperties(insert, properties, references);

//        List<OpenEngSBModelEntry> entries = insert.toOpenEngSBModelValues();
//        OpenEngSBModelEntry fullname     = entries.get(2);
//        OpenEngSBModelEntry phoneNumbers = entries.get(5);
//        OpenEngSBModelEntry performs     = entries.get(6);

        database.commit();
    }


    private void insertModel(OpenEngSBModel model, OrientVertex v_commit)
    {
        String modelClassName = model.getClass().getSimpleName();
        OrientVertex v_current = graph.addVertex("class:" + modelClassName);
        OrientVertex v_history = graph.addVertex("class:" + modelClassName + "History");
        OrientVertex v_revision = graph.addVertex("class:" + modelClassName + "Revision");

        v_current.setProperty("history", v_history);
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

    private OrientVertex createCommitVertex(EKBCommit commit, Date timestamp, OrientVertex v_parentCommit) {
        OrientVertex v_commit = graph.addVertex("class:Commit");

        v_commit.setProperty("timestamp", timestamp);
        v_commit.setProperty("domainId", commit.getDomainId());
        v_commit.setProperty("connectorId", commit.getConnectorId());
        v_commit.setProperty("instanceId", commit.getInstanceId());
        v_commit.setProperty("comment", commit.getComment());
        v_commit.setProperty("revisionNumber", commit.getRevisionNumber());

        if (v_parentCommit != null) {
            v_commit.setProperty("parentRevisionNumber", v_parentCommit.getProperty("revisionNumber"));
            v_commit.setProperty("parent", v_parentCommit);
            v_parentCommit.setProperty("next", v_commit);
        }

        return v_commit;
    }








































    @Override
    public void commit(EKBCommit ekbCommit, UUID headRevision) {

    }

    @Override
    public void addTransformation(TransformationDescriptor descriptor) {

    }

    @Override
    public <T> List<T> query(Query query) {
        return null;
    }

    @Override
    public Object nativeQuery(Object query) {
        return null;
    }

    @Override
    public UUID getLastRevisionId() {
        return null;
    }

    @Override
    public void deleteCommit(UUID headRevision) {

    }

    @Override
    public EKBCommit loadCommit(UUID revision) {
        return null;
    }

    @Override
    public <T> T getModel(Class<T> model, String oid) {
        return null;
    }
}