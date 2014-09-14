package org.openengsb.framework.ekb.persistence.orientdb.models;

import org.openengsb.core.api.model.annotation.Model;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */

@Model
public class Manager extends Person {

    private boolean isSeniorManager;

    public boolean isSeniorManager() {
        return isSeniorManager;
    }

    public void setSeniorManager(boolean isSeniorManager) {
        this.isSeniorManager = isSeniorManager;
    }
}