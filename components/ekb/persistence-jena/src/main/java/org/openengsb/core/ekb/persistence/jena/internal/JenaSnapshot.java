package org.openengsb.core.ekb.persistence.jena.internal;

import java.util.ArrayList;
import java.util.List;

import org.openengsb.core.ekb.persistence.jena.internal.api.OntoException;

import com.hp.hpl.jena.rdf.model.Resource;

public class JenaSnapshot extends JenaCommit {

    private final List<Resource> entities;

    public JenaSnapshot(String committer, String contextId) {
        super(committer, contextId);
        this.entities = new ArrayList<Resource>();
    }

    /**
     * get list of unchanged resource.
     * 
     * @return unchanged resources.
     */
    public List<Resource> getEntities() {
        return entities;
    }

    /**
     * Add new object into resource.
     * 
     * @param obj
     * @throws OntoException
     */
    public void entities(Resource obj) throws OntoException {
        entities.add(obj);
    }

}
