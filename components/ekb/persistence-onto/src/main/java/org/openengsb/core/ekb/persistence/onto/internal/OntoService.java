package org.openengsb.core.ekb.persistence.onto.internal;

import java.util.UUID;

import com.hp.hpl.jena.ontology.OntResource;

public interface OntoService {

    public void deleteCommit(UUID headRevision);

    public UUID getCurrentRevisionNumber();

    public Object executeQuery(String string, String contextId);

    public UUID getLastRevisionNumberOfContext(String contextId);

    public OntResource commit(OntoCommit commit);
}
