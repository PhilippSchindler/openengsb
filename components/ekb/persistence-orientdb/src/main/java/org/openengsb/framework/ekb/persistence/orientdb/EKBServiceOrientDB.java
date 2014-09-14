package org.openengsb.framework.ekb.persistence.orientdb;

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

    @Override
    public void commit(EKBCommit ekbCommit) {


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