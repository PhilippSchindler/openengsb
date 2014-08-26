package org.openengsb.core.ekb.persistence.jena.internal.api;

import java.util.List;
import java.util.UUID;

import org.openengsb.core.ekb.api.EKBCommit;
import org.openengsb.core.ekb.api.Query;
import org.openengsb.core.ekb.persistence.jena.internal.JenaCommit;

import com.hp.hpl.jena.ontology.OntResource;

public interface OntoService {

    public void deleteCommit(UUID headRevision);

    public UUID getCurrentRevisionNumber();

    public Object executeQuery(String string, String contextId);

    public <T> List<T> query(Query query);

    public UUID getLastRevisionNumberOfContext(String contextId);

    public OntResource commit(JenaCommit commit);

    public EKBCommit loadCommit(UUID revision);
}
