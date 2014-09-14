package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.core.api.model.OpenEngSBModelEntry;
import org.openengsb.core.ekb.api.EKBCommit;
import org.openengsb.core.ekb.api.EKBService;
import org.openengsb.core.ekb.api.Query;
import org.openengsb.core.ekb.api.TransformationDescriptor;

import java.util.List;
import java.util.UUID;

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

        List<OpenEngSBModel> inserts = ekbCommit.getInserts();
        OpenEngSBModel insert = inserts.get(0);

        List<OpenEngSBModelEntry> entries = insert.toOpenEngSBModelValues();
        OpenEngSBModelEntry fullname     = entries.get(2);
        OpenEngSBModelEntry phoneNumbers = entries.get(5);
        OpenEngSBModelEntry performs     = entries.get(6);

        ODocument document = database.newInstance("Person");
        document.field("fullname", "Peter");
        document.save();

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