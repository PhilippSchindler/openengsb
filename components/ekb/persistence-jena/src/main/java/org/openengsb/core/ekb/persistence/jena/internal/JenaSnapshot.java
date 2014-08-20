package org.openengsb.core.ekb.persistence.jena.internal;

import java.util.ArrayList;
import java.util.List;

import org.openengsb.core.ekb.persistence.jena.internal.api.OntoException;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class JenaSnapshot extends JenaCommit {

    private final List<RDFNode> entities;

    public JenaSnapshot(String committer, String contextId) {
        super(committer, contextId);
        this.entities = new ArrayList<RDFNode>();
    }

    public JenaSnapshot(Resource commitRes) {
        super(commitRes);

        Property entitiesProp = dataGraph.getProperty(JenaConstants.CDL_COMMIT_ENTITIES);
        List<RDFNode> entityNodes = dataGraph.listObjectsOfProperty(entitiesProp).toList();
        this.entities = new ArrayList<RDFNode>(entityNodes);
    }

    /**
     * get list of unchanged resource.
     * 
     * @return unchanged resources.
     */
    public List<RDFNode> getEntities() {
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
