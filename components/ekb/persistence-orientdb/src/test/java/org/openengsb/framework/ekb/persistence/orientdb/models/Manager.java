package org.openengsb.framework.ekb.persistence.orientdb.models;

import org.openengsb.core.api.model.annotation.Model;

import java.util.List;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */

@Model
public class Manager extends Person {

    private boolean isSeniorManager;
    private List<String> departments;

    public boolean isSeniorManager() {
        return isSeniorManager;
    }

    public void setSeniorManager(boolean isSeniorManager) {
        this.isSeniorManager = isSeniorManager;
    }

    public List<String> getDepartments() {
        return departments;
    }

    public void setDepartments(List<String> departments) {
        this.departments = departments;
    }
}