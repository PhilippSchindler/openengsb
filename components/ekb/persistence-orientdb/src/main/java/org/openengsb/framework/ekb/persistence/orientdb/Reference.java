package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.core.metadata.schema.OClass;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */
public class Reference {

    private final OClass from;
    private final String name;
    private final Class<?> to;

    public OClass getFrom() {
        return from;
    }

    public String getName() {
        return name;
    }

    public Class<?> getTo() {
        return to;
    }

    public Reference(OClass from, String name, Class<?> to) {
        this.from = from;
        this.name = name;
        this.to = to;
    }
}
