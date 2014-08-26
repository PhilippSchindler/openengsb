package org.openengsb.core.ekb.api;


public interface QueryFilter {
    boolean filter(Object... objects);
    Object getQueryFilterElement(); 
    void setQueryFilterElement(Object object); 
}
