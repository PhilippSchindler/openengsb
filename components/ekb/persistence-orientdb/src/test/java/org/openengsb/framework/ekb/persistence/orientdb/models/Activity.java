package org.openengsb.framework.ekb.persistence.orientdb.models;

import org.openengsb.core.api.model.annotation.Model;
import org.openengsb.core.api.model.annotation.OpenEngSBModelId;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */

@Model
public class Activity {

    @OpenEngSBModelId
    private String id;

    private String desciption;
    private boolean finished;
    private int duration;
    private int expectedDuration;
}