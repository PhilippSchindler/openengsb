package org.openengsb.core.ekb.persistence.onto.internal;

import java.util.UUID;

public interface OntoService {

    public void deleteCommit(UUID headRevision);

    public UUID getCurrentRevisionNumber();

    public Object executeQuery(String string);

    public UUID getLastRevisionNumberOfContext(String contextId);
}
