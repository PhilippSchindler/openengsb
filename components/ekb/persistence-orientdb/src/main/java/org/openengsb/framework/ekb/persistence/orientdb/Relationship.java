package org.openengsb.framework.ekb.persistence.orientdb;

import org.openengsb.core.api.model.OpenEngSBModel;

/**
 * Created by Philipp Schindler on 16.09.2014.
 */
public interface Relationship {

    public String getRID();
    public void setRID(String rid);

    public String getName();
    public OpenEngSBModel[] getRelatedModels();

    public String getLinkNameForRecentRevision(OpenEngSBModel model);
    public String getLinkNameForAllRevisions(OpenEngSBModel model);
}
