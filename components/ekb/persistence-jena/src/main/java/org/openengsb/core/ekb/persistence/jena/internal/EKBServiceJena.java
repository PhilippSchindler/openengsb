package org.openengsb.core.ekb.persistence.jena.internal;

import java.util.List;
import java.util.UUID;

import jline.internal.Log;

import org.openengsb.core.api.context.ContextHolder;
import org.openengsb.core.api.security.AuthenticationContext;
import org.openengsb.core.ekb.api.EKBCommit;
import org.openengsb.core.ekb.api.EKBConcurrentException;
import org.openengsb.core.ekb.api.EKBException;
import org.openengsb.core.ekb.api.EKBService;
import org.openengsb.core.ekb.api.Query;
import org.openengsb.core.ekb.api.TransformationDescriptor;
import org.openengsb.core.ekb.api.hooks.EKBErrorHook;
import org.openengsb.core.ekb.api.hooks.EKBPostCommitHook;
import org.openengsb.core.ekb.api.hooks.EKBPreCommitHook;
import org.openengsb.core.ekb.persistence.jena.internal.api.OntoService;

import com.google.common.base.Objects;

/**
 * Hello world!
 * 
 */
public class EKBServiceJena implements EKBService {
    // private static final Logger LOGGER =
    // LoggerFactory.getLogger(EKBServiceJena.class);

    private final OntoService jenaService;
    private final JenaConverter ontoConverter;
    private final List<EKBPreCommitHook> preCommitHooks;
    private final List<EKBPostCommitHook> postCommitHooks;
    private final List<EKBErrorHook> errorHooks;
    private final AuthenticationContext authContext;

    public EKBServiceJena(OntoService ontoService, JenaConverter ontoConverter, List<EKBPreCommitHook> preCommitHooks,
            List<EKBPostCommitHook> postCommitHooks, List<EKBErrorHook> errorHooks, AuthenticationContext authContext) {
        this.jenaService = ontoService;
        this.ontoConverter = ontoConverter;
        this.preCommitHooks = preCommitHooks;
        this.postCommitHooks = postCommitHooks;
        this.errorHooks = errorHooks;
        this.authContext = authContext;
    }

    @Override
    public void commit(EKBCommit ekbCommit) {
        Log.debug("Commit of models was called");
        runPersistingLogic(ekbCommit, null, false);
        Log.debug("Commit of models was successful");
    }

    /**
     * Runs the logic of the PersistInterface. Does the sanity checks if check
     * is set to true. Additionally tests if the head revision of the context
     * under which the commit is performed has the given revision number if the
     * headRevisionCheck flag is set to true.
     */
    private void runPersistingLogic(EKBCommit commit, UUID expectedContextHeadRevision, boolean headRevisionCheck)
            throws EKBException {
        String contextId = ContextHolder.get().getCurrentContextId();

        if (headRevisionCheck) {
            checkForContextHeadRevision(contextId, expectedContextHeadRevision);
        }
        runEKBPreCommitHooks(commit);
        EKBException exception = null;
        JenaCommit converted = ontoConverter.convertEKBCommit(commit, authContext);
        try {
            performPersisting(converted, commit);
            runEKBPostCommitHooks(commit);
        } catch (EKBException e) {
            exception = e;
        }
        runEKBErrorHooks(commit, exception);
    }

    /**
     * Performs the persisting of the models into the EDB.
     */
    private void performPersisting(JenaCommit commit, EKBCommit source) {
        try {
            Log.info("performPersisting: " + commit);
            jenaService.commit(commit);
            source.setRevisionNumber(commit.getRevision());
            source.setParentRevisionNumber(commit.getParentRevision());
        } catch (Exception e) {
            throw new EKBException("Error while commiting EKBCommit", e);
        }
    }

    /**
     * Tests if the head revision for the given context matches the given
     * revision number. If this is not the case, an EKBConcurrentException is
     * thrown.
     */
    private void checkForContextHeadRevision(String contextId, UUID expectedHeadRevision) throws EKBConcurrentException {
        if (!Objects.equal(jenaService.getLastRevisionNumberOfContext(contextId), expectedHeadRevision)) {
            throw new EKBConcurrentException("The current revision of the context does not match the "
                    + "expected one.");
        }
    }

    /**
     * Runs all registered pre-commit hooks
     */
    private void runEKBPreCommitHooks(EKBCommit commit) throws EKBException {
        for (EKBPreCommitHook hook : preCommitHooks) {
            try {
                hook.onPreCommit(commit);
            } catch (EKBException e) {
                throw new EKBException("EDBException is thrown in a pre commit hook.", e);
            } catch (Exception e) {
                Log.warn("An exception is thrown in a EKB pre commit hook.", e);
            }
        }
    }

    /**
     * Runs all registered post-commit hooks
     */
    private void runEKBPostCommitHooks(EKBCommit commit) throws EKBException {
        for (EKBPostCommitHook hook : postCommitHooks) {
            try {
                hook.onPostCommit(commit);
            } catch (Exception e) {
                Log.warn("An exception is thrown in a EKB post commit hook.", e);
            }
        }
    }

    /**
     * Runs all registered error hooks
     */
    private void runEKBErrorHooks(EKBCommit commit, EKBException exception) {
        if (exception != null) {
            for (EKBErrorHook hook : errorHooks) {
                hook.onError(commit, exception);
            }
            throw exception;
        }
    }

    @Override
    public void commit(EKBCommit ekbCommit, UUID headRevision) {
        Log.debug("Commit of models was called");
        runPersistingLogic(ekbCommit, headRevision, true);
        Log.debug("Commit of models was successful");
    }

    @Override
    public void deleteCommit(UUID headRevision) {
        String contextId = ContextHolder.get().getCurrentContextId();
        if (headRevision == null || contextId == null) {
            throw new EKBException("null revision or context not allowed");
        }

        checkHeadRevision(headRevision);
        jenaService.deleteCommit(headRevision);
    }

    private void checkHeadRevision(UUID expectedHeadRevision) {
        if (!Objects.equal(jenaService.getCurrentRevisionNumber(), expectedHeadRevision)) {
            throw new EKBConcurrentException("The current revision of the context does not match the "
                    + "expected one.");
        }
    }

    @Override
    public void addTransformation(TransformationDescriptor descriptor) {
        // TODO Auto-generated method stub
    }

    @Override
    public <T> List<T> query(Query query) {
    	return jenaService.query(query);
    }

    @Override
    public Object nativeQuery(Object query) {
        return jenaService.executeQuery(query.toString(), ContextHolder.get().getCurrentContextId());
    }

    @Override
    public UUID getLastRevisionId() {
        return jenaService.getCurrentRevisionNumber();
    }

    @Override
    public EKBCommit loadCommit(UUID revision) {
        return jenaService.loadCommit(revision);
    }

    @Override
    public <T> T getModel(Class<T> model, String oid) {
        // TODO Auto-generated method stub
        return null;
    }
}
