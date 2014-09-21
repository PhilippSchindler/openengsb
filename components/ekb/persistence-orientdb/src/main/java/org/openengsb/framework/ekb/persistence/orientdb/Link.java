package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Created by Philipp Schindler on 21.09.2014.
 */
public class Link {

    private String name;
    private ODocument target;

    public String getName() {
        return name;
    }

    public ODocument getTarget() {
        return target;
    }

    public Link(String name, ODocument target) {
        this.name = name;
        this.target = target;
    }
}
