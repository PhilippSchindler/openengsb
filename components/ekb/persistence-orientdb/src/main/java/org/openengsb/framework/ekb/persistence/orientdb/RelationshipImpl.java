package org.openengsb.framework.ekb.persistence.orientdb;

import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.core.api.model.annotation.Model;

/**
 * Created by Philipp Schindler on 20.09.2014.
 */

@Model
public class RelationshipImpl implements Relationship {

    private String name;
    private String RID;
    private OpenEngSBModel[] relatedModels;

    public RelationshipImpl(String rid) {
        this.RID = rid;
    }

    public RelationshipImpl(String name, Object... relatedModels) {
        this.name = name;
        this.relatedModels = new OpenEngSBModel[relatedModels.length];
        for (int i = 0; i < relatedModels.length; i++)
            this.relatedModels[i] = (OpenEngSBModel)relatedModels[i];
    }

    @Override
    public String getRID() {
        return RID;
    }

    @Override
    public void setRID(String rid) {
        this.RID = rid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public OpenEngSBModel[] getRelatedModels() {
        return relatedModels;
    }

    @Override
    public String getLinkNameForRecentRevision(OpenEngSBModel model) {
        String className = model.getClass().getSimpleName();
        return className.substring(0, 1).toLowerCase() + className.substring(1);
    }

    @Override
    public String getLinkNameForAllRevisions(OpenEngSBModel model) {
        return getLinkNameForRecentRevision(model) + "Revisions";
    }
}