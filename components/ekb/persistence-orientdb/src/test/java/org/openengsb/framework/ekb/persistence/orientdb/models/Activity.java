package org.openengsb.framework.ekb.persistence.orientdb.models;

import org.openengsb.core.api.model.annotation.Model;
import org.openengsb.core.api.model.annotation.OpenEngSBModelId;
import org.openengsb.framework.ekb.persistence.orientdb.Relationship;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */

@Model
public class Activity {

    @OpenEngSBModelId
    private String uiid;

    private String RID;
    private String desciption;
    private boolean finished;
    private int duration;
    private int expectedDuration;

    @Relationship
    private Project belongsTo;

    @Relationship
    private Manager isSpecifiedBy;

    public String getUiid() {
        return uiid;
    }

    public void setUiid(String uiid) {
        this.uiid = uiid;
    }

    public String getRID() {
        return RID;
    }

    public void setRID(String RID) {
        this.RID = RID;
    }

    public String getDesciption() {
        return desciption;
    }

    public void setDesciption(String desciption) {
        this.desciption = desciption;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getExpectedDuration() {
        return expectedDuration;
    }

    public void setExpectedDuration(int expectedDuration) {
        this.expectedDuration = expectedDuration;
    }


    public Project getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(Project belongsTo) {
        this.belongsTo = belongsTo;
    }

    public Manager getIsSpecifiedBy() {
        return isSpecifiedBy;
    }

    public void setIsSpecifiedBy(Manager isSpecifiedBy) {
        this.isSpecifiedBy = isSpecifiedBy;
    }
}