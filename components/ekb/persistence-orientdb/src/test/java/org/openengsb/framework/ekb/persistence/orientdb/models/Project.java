package org.openengsb.framework.ekb.persistence.orientdb.models;

import org.openengsb.core.api.model.annotation.Model;
import org.openengsb.core.api.model.annotation.OpenEngSBModelId;

import java.util.Date;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */

@Model
public class Project {

    @OpenEngSBModelId
    private String id;

    private String name;
    private Date startDate;
    private Date endDate;
    private Date plannedEndDate;
    private String stage;
}