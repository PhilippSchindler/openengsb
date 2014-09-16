package org.openengsb.framework.ekb.persistence.orientdb.models;

import org.openengsb.core.api.model.annotation.Model;
import org.openengsb.core.api.model.annotation.OpenEngSBModelId;
import org.openengsb.framework.ekb.persistence.orientdb.Relationship;

import java.util.Date;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */

@Model
public class Project {

    @OpenEngSBModelId
    private String uiid;

    private String RID;
    private String name;
    private Date startDate;
    private Date endDate;
    private Date plannedEndDate;
    private String stage;

    @Relationship
    private Project hasManager;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getPlannedEndDate() {
        return plannedEndDate;
    }

    public void setPlannedEndDate(Date plannedEndDate) {
        this.plannedEndDate = plannedEndDate;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }


    public Project getHasManager() {
        return hasManager;
    }

    public void setHasManager(Project hasManager) {
        this.hasManager = hasManager;
    }
}