package org.openengsb.framework.ekb.persistence.orientdb;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */
public class Reference {

    private final String from;
    private final String name;
    private final String to;

    public String getFrom() {
        return from;
    }

    public String getName() {
        return name;
    }

    public String getTo() {
        return to;
    }

    public Reference(String from, String name, String to) {
        this.from = from;
        this.name = name;
        this.to = to;
    }
}
