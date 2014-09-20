package org.openengsb.framework.ekb.persistence.orientdb;

import org.openengsb.core.api.model.OpenEngSBModel;

/**
 * Created by Philipp Schindler on 20.09.2014.
 */
public class Operation {

    private OperationType type;
    private OpenEngSBModel model;

    public OperationType getType() {
        return type;
    }
    public OpenEngSBModel getModel() {
        return model;
    }

    /**
     * Create a new operation for use in an EKBCommit.
     * Check's if model is a OpenEngSBModel and throws an IllegalArgumentException otherwise
     * @param type
     * @param model
     */
    public Operation(OperationType type, Object model) {
        this.type = type;
        checkIfModel(model);
        this.model = (OpenEngSBModel)model;
    }

    /**
     * Checks if an object is an OpenEngSBModel and throws an IllegalArgumentException if the object is no model.
     */
    private void checkIfModel(Object model) {
        if (!OpenEngSBModel.class.isAssignableFrom(model.getClass())) {
            throw new IllegalArgumentException("Only models can be committed");
        }
    }
}

