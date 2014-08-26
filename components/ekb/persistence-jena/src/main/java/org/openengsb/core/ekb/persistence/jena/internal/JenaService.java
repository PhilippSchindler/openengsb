package org.openengsb.core.ekb.persistence.jena.internal;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jline.internal.Log;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ClassUtils;
import org.apache.jena.riot.RDFDataMgr;
import org.openengsb.core.api.context.ContextHolder;
import org.openengsb.core.api.model.FileWrapper;
import org.openengsb.core.api.model.ModelDescription;
import org.openengsb.core.api.model.OpenEngSBModel;
import org.openengsb.core.api.model.OpenEngSBModelEntry;
import org.openengsb.core.api.model.QueryRequest;
import org.openengsb.core.ekb.api.EKBCommit;
import org.openengsb.core.ekb.api.ModelRegistry;
import org.openengsb.core.ekb.api.Query;
import org.openengsb.core.ekb.api.QueryFilter;
import org.openengsb.core.ekb.api.QueryProjection;
import org.openengsb.core.ekb.persistence.jena.internal.api.OntoService;
import org.openengsb.core.ekb.persistence.jena.internal.api.OwlHelper;
import org.openengsb.core.util.ModelUtils;

import com.google.common.collect.Lists;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Filter;

public class JenaService implements OntoService {

    public static final String REFERENCE_PREFIX = "refersTo_";

    private final Dataset dataset;
    private final Model template;
    private ModelRegistry modelRegistry;

    /**
     * Create a new JenaService.
     * 
     * @param dataset Main Storage.
     * @param defaultModel Root model containing information about context &
     *        commit.
     * @param isReset whether the existing data have to be reset.
     */
    public JenaService(Dataset dataset, Model defaultModel, Boolean isReset) {
        Log.info("Initialize OntoService Dataset");
        if (defaultModel != null) {
            this.template = defaultModel;
        } else {
            this.template = RDFDataMgr.loadModel(JenaConstants.CDL_TEMPLATE);
        }
        this.dataset = dataset;
        if (isReset) {
            resetDataset();
            Log.info("Reset OntoService Dataset");
        }
    }

    /**
     * Erase everything in the ontology. Proceed with your own risk!
     */
    private void resetDataset() {
        dataset.begin(ReadWrite.WRITE);

        Iterator<String> namedGraphs = dataset.listNames();
        while (namedGraphs.hasNext()) {
            String namedGraph = namedGraphs.next();
            dataset.removeNamedModel(namedGraph);
        }
        Model model = dataset.getDefaultModel();
        model.removeAll();
        model.add(template);
        model.setNsPrefixes(template.getNsPrefixMap());

        dataset.commit();
        dataset.end();
    }

    @Override
    public void deleteCommit(UUID headRevision) {
        dataset.begin(ReadWrite.WRITE);

        Model model = dataset.getDefaultModel();
        String context = ContextHolder.get().getCurrentContextId();
        Property contextHeadProperty = model.getProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT);
        Resource contextRes = model.getResource(JenaConstants.CDL_NAMESPACE + context);
        Resource commit = model.getProperty(contextRes, contextHeadProperty).getObject().asResource();

        if (headRevision != null) {
            UUID revUUID = UUID.fromString(commit.getLocalName());
            System.out.println(revUUID);
            if (!revUUID.equals(headRevision)) {
                Log.info("The head revision is not the same with the latest revision");
                return;
            }
        }

        System.out.println("delete done 1");

        Property commitParentRevision = model.getProperty(JenaConstants.CDL_COMMIT_PARENT_REVISION);
        Statement stmt = commit.getProperty(commitParentRevision);
        if (stmt == null) {
            model.removeAll(contextRes, null, null);
        } else {
            Resource prevCommit = stmt.getObject().asResource();
            contextRes.removeAll(contextHeadProperty);
            contextRes.addProperty(contextHeadProperty, prevCommit);
            model.remove(prevCommit, model.getProperty(JenaConstants.CDL_COMMIT_CHILD_REVISION), commit);
        }
        System.out.println("delete done 2");

        ExtendedIterator<Statement> iter = commit.listProperties(model.getProperty(JenaConstants.CDL_COMMIT_INSERTS))
                .andThen(commit.listProperties(model.getProperty(JenaConstants.CDL_COMMIT_UPDATES)));

        System.out.println("delete done 3");
        while (iter.hasNext()) {
            Resource node = iter.next().getObject().asResource();
            System.out.println("DEL: " + node.getURI());
            model.removeAll(node, null, null);
        }
        System.out.println("delete done 4");

        model.removeAll(commit, null, null);
        System.out.println("delete done 5");

        OwlHelper.save(model, "src/test/resources/test-delete.owl");

        dataset.commit();
        dataset.end();
    }

    @Override
    public UUID getCurrentRevisionNumber() {
        dataset.begin(ReadWrite.READ);

        Model model = dataset.getDefaultModel();
        String context = ContextHolder.get().getCurrentContextId();
        Resource contextRes = model.getResource(JenaConstants.CDL_NAMESPACE + context);
        Property contextHead = model.getProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT);
        Resource commit = model.getProperty(contextRes, contextHead).getObject().asResource();
        Property commitRev = model.getProperty(JenaConstants.CDL_COMMIT_REVISION);
        RDFNode node = commit.getProperty(commitRev).getObject();
        String revisionId = node.asLiteral().getString();

        UUID revUUID = UUID.fromString(revisionId);
        dataset.end();

        return revUUID;
    }

    @Override
    public Object executeQuery(String string, String contextId) {
        dataset.begin(ReadWrite.READ);

        Model model = dataset.getDefaultModel();
        ParameterizedSparqlString query = new ParameterizedSparqlString(string);
        query.setNsPrefixes(model.getNsPrefixMap());
        QueryExecution qe = QueryExecutionFactory.create(query.toString(), model);
        ResultSet rs = qe.execSelect();
        OutputStream os = new ByteArrayOutputStream();
        ResultSetFormatter.outputAsJSON(os, rs);
        qe.close();

        dataset.end();

        return os;
    }

    @Override
    public UUID getLastRevisionNumberOfContext(String contextId) {
        UUID uuid = null;
        dataset.begin(ReadWrite.READ);

        Model model = dataset.getDefaultModel();
        Resource contextRes = model.getResource(JenaConstants.CDL_NAMESPACE + contextId);
        if (contextRes != null) {
            Property contextHead = model.getProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT);
            Statement stmt = model.getProperty(contextRes, contextHead);
            if (stmt == null) {
                RDFNode revision = model.getProperty(contextRes, contextHead).getObject();
                uuid = UUID.fromString(revision.asLiteral().getString());
            }
        }

        dataset.end();

        return uuid;
    }

    @Override
    public OntResource commit(JenaCommit commit) {
        String contextId = commit.getContext();
        UUID commitID = commit.getRevision();
        commit.setChildRevision(null);

        String contextURI = JenaConstants.CDL_NAMESPACE + contextId;
        String commitURI = JenaConstants.CDL_NAMESPACE + commitID;

        dataset.begin(ReadWrite.WRITE);

        OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, dataset.getDefaultModel());

        Resource infoContextCls = ontModel.getResource(JenaConstants.CDL_CONTEXT);
        Resource infoCommitCls = ontModel.getResource(JenaConstants.CDL_COMMIT);

        Individual commitInstance = ontModel.createIndividual(commitURI, infoCommitCls);
        Individual contextInstance = ontModel.getIndividual(contextURI);
        Resource parentCommitInstance = null;

        Property insertProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_INSERTS);
        Property updateProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_UPDATES);
        Property deleteProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_DELETES);
        Property entityProperty = ontModel.getProperty(JenaConstants.CDL_COMMIT_ENTITIES);

        if (contextInstance == null) {
            contextInstance = ontModel.createIndividual(contextURI, infoContextCls);
            DatatypeProperty contextProp = ontModel.createDatatypeProperty(JenaConstants.CDL_CONTEXT_ID);
            contextInstance.addProperty(contextProp, contextId);

            commit.setParentRevision(null);

        } else {
            parentCommitInstance = contextInstance.getPropertyResourceValue(ontModel
                    .createProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT));
            parentCommitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_CHILD_REVISION),
                    commitInstance);
            commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_PARENT_REVISION),
                    parentCommitInstance);
            commit.setParentRevision(UUID.fromString(parentCommitInstance.getLocalName()));
            NodeIterator nodeIter = ontModel.listObjectsOfProperty(parentCommitInstance, entityProperty);
            while (nodeIter.hasNext()) {
                ontModel.add(commitInstance, entityProperty, nodeIter.next());
            }
        }

        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_CONTEXT), contextInstance);

        contextInstance
                .setPropertyValue(ontModel.createProperty(JenaConstants.CDL_CONTEXT_HEAD_COMMIT), commitInstance);

        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_REVISION), commitID.toString());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_COMMITTER), commit.getCommitter());
        commitInstance
                .addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_CONNECTOR_ID), commit.getConnectorId());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_DOMAIN_ID), commit.getDomainId());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_INSTANCE_ID), commit.getInstanceId());
        commitInstance.addProperty(ontModel.getProperty(JenaConstants.CDL_COMMIT_TIMESTAMP),
                ontModel.createTypedLiteral(commit.getTimestamp()));

        Model commitModel = commit.getDataGraph();

        Property oid = commitModel.getProperty(JenaConstants.CDL_OID);
        Property provWasRevisionOf = commitModel.getProperty(JenaConstants.PROV_REVISION);

        if (commit.getInserts() != null || !commit.getInserts().isEmpty()) {
            Iterator<RDFNode> inserts = commit.getInserts().iterator();

            while (inserts.hasNext()) {
                RDFNode insert = inserts.next();

                commitModel.add(commitInstance, insertProperty, insert);
                commitModel.add(commitInstance, entityProperty, insert);
            }
        }

        if (commit.getUpdates() != null || !commit.getUpdates().isEmpty()) {
            Iterator<RDFNode> updates = commit.getUpdates().iterator();
        	Log.info("Update-Remove1");

            while (updates.hasNext()) {
                Resource update = updates.next().asResource();
                Statement stmt = update.getProperty(oid);

                Filter<Statement> filter = new JenaOidFilter(oid, stmt.getObject());
                ExtendedIterator<Statement> iter = parentCommitInstance.listProperties(entityProperty).filterKeep(
                        filter);
            	Log.info("Update-Remove2", update.getURI());
                if (iter.hasNext()) {
                    // link to previous version of data & remove old object from
                    // the entity list
                    Resource oldObject = iter.next().getObject().asResource();
                    commitModel.add(update, provWasRevisionOf, oldObject);
                    ontModel.remove(commitInstance, entityProperty, oldObject);
                	Log.info("Update-Remove3", oldObject.getURI());
                }

                commitModel.add(commitInstance, updateProperty, update);
                commitModel.add(commitInstance, entityProperty, update);
            }
            OwlHelper.save(commitModel, "src/test/resources/test-update.owl");
        }

        if (commit.getDeletes() != null || !commit.getDeletes().isEmpty()) {
            Iterator<RDFNode> deleteModel = commit.getDeletes().iterator();

            while (deleteModel.hasNext()) {
                RDFNode delete = deleteModel.next();

                Filter<Statement> filter = new JenaOidFilter(oid, delete);
                ExtendedIterator<Statement> iter = parentCommitInstance.listProperties(entityProperty).filterKeep(
                        filter);

                if (iter.hasNext()) {
                    Resource dataToBeDeleted = iter.next().getSubject();
                    commitModel.add(commitInstance, deleteProperty, dataToBeDeleted);
                    ontModel.remove(commitInstance, entityProperty, dataToBeDeleted);
                }
            }
        }

        ontModel.add(commitModel);

        OwlHelper.save(ontModel, "src/test/resources/test-main.owl");

        dataset.commit();
        dataset.end();

        return null;
    }

    public static class JenaOidFilter extends Filter<Statement> {
        private final Property p;
        private final RDFNode oid;

        public JenaOidFilter(Property p, RDFNode oid) {
            this.p = p;
            this.oid = oid;
        }

        public JenaOidFilter(Property p, String oid) {
            this.p = p;
            this.oid = ResourceFactory.createPlainLiteral(oid);
        }

        @Override
        public boolean accept(Statement o) {
            Resource obj = o.getObject().asResource();
            if (obj.getProperty(p).getObject().equals(oid)) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public EKBCommit loadCommit(UUID commitID) {
        JenaCommit snapshot = loadJenaCommit(commitID);
        EKBCommit commit = loadCommitFromSnapshot(snapshot);

        return commit;
    }

    public JenaCommit loadJenaCommit(UUID commitID) {
        JenaCommit snapshot = null;

        dataset.begin(ReadWrite.READ);
        Model model = dataset.getDefaultModel();
        String URI = JenaConstants.CDL_NAMESPACE + commitID;

        Resource commitInstance = model.getResource(URI);
        if (model.containsResource(commitInstance)) {
            snapshot = new JenaCommit(commitInstance);
        }

        dataset.end();

        return snapshot;
    }

    public EKBCommit loadCommitFromSnapshot(JenaCommit commit) {

        System.out.println(commit);

        EKBCommit result = new EKBCommit();
        Map<ModelDescription, Class<?>> cache = new HashMap<>();

        result.setRevisionNumber(commit.getRevision());
        result.setParentRevisionNumber(commit.getParentRevision());
        result.setComment((commit.getComment() != null) ? commit.getComment() : "");
        result.setDomainId((commit.getDomainId() != null) ? commit.getDomainId() : "");
        result.setConnectorId((commit.getConnectorId() != null) ? commit.getConnectorId() : "");
        result.setInstanceId((commit.getInstanceId() != null) ? commit.getInstanceId() : "");

        for (RDFNode insert : commit.getInserts()) {
            result.addInsert(createModelOfResource(insert, cache));
        }
        for (RDFNode update : commit.getUpdates()) {
            result.addUpdate(createModelOfResource(update, cache));
        }
        for (RDFNode delete : commit.getDeletes()) {
            dataset.begin(ReadWrite.READ);

            Model model = dataset.getDefaultModel();
            Resource commitRes = model.getResource(JenaConstants.CDL_NAMESPACE + commit.getRevision());
            Property deleteList = model.getProperty(JenaConstants.CDL_COMMIT_DELETES);
            Property oid = model.getProperty(JenaConstants.CDL_OID);
            ExtendedIterator<Statement> deleteListIter = commitRes.listProperties(deleteList);
            Statement stmt = deleteListIter.filterKeep(new JenaOidFilter(oid, delete)).next();

            if (stmt != null && stmt.getObject().isResource()) {
                Resource object = stmt.getObject().asResource();
                result.addDelete(createModelOfResource(object, cache));
            }

            dataset.end();
        }

        return result;
    }

    private Object createModelOfResource(RDFNode rdfNode, Map<ModelDescription, Class<?>> cache) {
        try {
            if (!rdfNode.isResource()) {
                throw new JenaException();
            }

            Resource node = rdfNode.asResource();
            ModelDescription description = getDescriptionFromObject(node);
            Log.info("ModelDescription: " + description);
            Class<?> modelClass;
            if (cache.containsKey(description)) {
                modelClass = cache.get(description);
            } else {
                modelClass = modelRegistry.loadModel(description);
                cache.put(description, modelClass);
            }
            return convertResourceToUncheckedModel(modelClass, node);
        } catch (IllegalArgumentException | ClassNotFoundException e) {
            Log.warn("Unable to create model of the object {} ", rdfNode.toString(), e);
            return null;
        } catch (JenaException e) {
            Log.warn("Node {} is not a resource", rdfNode.toString(), e);
            return null;
        }
    }

    /**
     * Converts an Resource to a model by analyzing the object and trying to
     * call the corresponding setters of the model.
     */
    private Object convertResourceToUncheckedModel(Class<?> model, Resource object) {
        // TODO: check if this function is necessary
        // filterEngineeringObjectInformation(object, model);

        List<OpenEngSBModelEntry> entries = new ArrayList<>();
		Log.info("convertResourceToUncheckedModel1");

        for (PropertyDescriptor propertyDescriptor : getPropertyDescriptorsForClass(model)) {
    		Log.info("convertResourceToUncheckedModel2");
            if (propertyDescriptor.getWriteMethod() == null
                    || propertyDescriptor.getName().equals(ModelUtils.MODEL_TAIL_FIELD_NAME)) {
                continue;
            }
    		Log.info("convertResourceToUncheckedModel2.1");

            Object value = getValueForProperty(propertyDescriptor, object);
            Class<?> propertyClass = propertyDescriptor.getPropertyType();
            if (propertyClass.isPrimitive()) {
                entries.add(new OpenEngSBModelEntry(propertyDescriptor.getName(), value, ClassUtils
                        .primitiveToWrapper(propertyClass)));
            } else {
                entries.add(new OpenEngSBModelEntry(propertyDescriptor.getName(), value, propertyClass));
            }
        }

        // TODO: again, check whether this function is necessary
        // for (Map.Entry<String, EDBObjectEntry> objectEntry :
        // object.entrySet()) {
        // EDBObjectEntry entry = objectEntry.getValue();
        // Class<?> entryType;
        // try {
        // entryType = model.getClassLoader().loadClass(entry.getType());
        // entries.add(new OpenEngSBModelEntry(entry.getKey(), entry.getValue(),
        // entryType));
        // } catch (ClassNotFoundException e) {
        // Log.error("Unable to load class {} of the model tail",
        // entry.getType());
        // }
        // }

        return ModelUtils.createModel(model, entries);
    }

    /**
     * Generate the value for a specific property of a model out of an
     * EDBObject.
     */
    private Object getValueForProperty(PropertyDescriptor propertyDescriptor, Resource object) {
        Model model = object.getModel();

        Method setterMethod = propertyDescriptor.getWriteMethod();
        String propertyName = propertyDescriptor.getName();

        Property propertyObj = model.getProperty(JenaConstants.CDL_NAMESPACE + propertyName);
        if (propertyObj == null)
            return null;
        Statement stmt = object.getProperty(propertyObj);
        if (stmt == null)
            return null;

        Object value = stmt.getObject();
        Class<?> parameterType = setterMethod.getParameterTypes()[0];

        // TODO: Check whether supports for type Map is necessary
        // series
        // if (Map.class.isAssignableFrom(parameterType)) {
        // List<Class<?>> classes = getGenericMapParameterClasses(setterMethod);
        // value = getMapValue(classes.get(0), classes.get(1), propertyName,
        // object);
        // } else

        if (List.class.isAssignableFrom(parameterType)) {
            Class<?> clazz = getGenericListParameterClass(setterMethod);
            value = getListValue(clazz, propertyName, object);
        } else if (parameterType.isArray()) {
            Class<?> clazz = parameterType.getComponentType();
            value = getArrayValue(clazz, propertyName, object);
        } else if (value == null) {
            return null;
        } else if (OpenEngSBModel.class.isAssignableFrom(parameterType)) {
            RDFNode valueRDF = (RDFNode) value;
            if (valueRDF.isResource()) {
                value = convertResourceToUncheckedModel(parameterType, valueRDF.asResource());
            } else {
                throw new JenaException("Type mismatch, should be Resource instead of Literal");
            }
        } else if (parameterType.equals(FileWrapper.class)) {
            FileWrapper wrapper = new FileWrapper();
            Property wrapperProperty = model.getProperty(JenaConstants.CDL_NAMESPACE + propertyName
                    + JenaConverter.FILEWRAPPER_FILENAME_SUFFIX);
            if (wrapperProperty == null)
                return null;
            Statement wrapperStmt = object.getProperty(wrapperProperty);
            if (wrapperStmt == null || !wrapperStmt.getObject().isLiteral())
                return null;

            String filename = wrapperStmt.getObject().asLiteral().getString();

            String content = (String) value;
            wrapper.setFilename(filename);
            wrapper.setContent(Base64.decodeBase64(content));
            value = wrapper;
        } else if (parameterType.equals(File.class)) {
            return null;
        } else {
            value = ((RDFNode) value).visitWith(new JenaVisitor());
        }

        return value;
    }

    /**
     * Gets an array object out of an EDBObject.
     */
    @SuppressWarnings("unchecked")
    private <T> T[] getArrayValue(Class<T> type, String propertyName, Resource object) {
        List<T> elements = getListValue(type, propertyName, object);
        T[] ar = (T[]) Array.newInstance(type, elements.size());
        return elements.toArray(ar);
    }

    /**
     * Gets a list object out of an EDBObject.
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getListValue(Class<T> type, String propertyName, Resource object) {
        List<T> temp = new ArrayList<>();
        Model m = object.getModel();

        Property p = m.getProperty(JenaConstants.CDL_NAMESPACE + propertyName);
        if (p == null)
            return temp;
        NodeIterator objs = m.listObjectsOfProperty(object, p);
        while (objs.hasNext()) {
            RDFNode node = objs.next();
            if (node == null) {
                break;
            } else if (node.isResource()) {
                Object obj = convertResourceToUncheckedModel(type, node.asResource());
                temp.add((T) obj);
            } else if (node.isLiteral()) {
                temp.add((T) node.asLiteral().getValue());
            }
        }
        return temp;
    }

    /**
     * Returns the entry name for a list element in the EDB format. E.g. the
     * list element for the property "list" with the index 0 would be "list.0".
     */
    public static String getEntryNameForList(String property, Integer index) {
        return String.format("%s.%d", property, index);
    }

    /**
     * Get the type of the list parameter of a setter.
     */
    private Class<?> getGenericListParameterClass(Method setterMethod) {
        return getGenericParameterClasses(setterMethod, 1).get(0);
    }

    /**
     * Loads the generic parameter classes up to the given depth (1 for lists, 2
     * for maps)
     */
    private List<Class<?>> getGenericParameterClasses(Method setterMethod, int depth) {
        Type t = setterMethod.getGenericParameterTypes()[0];
        ParameterizedType pType = (ParameterizedType) t;
        List<Class<?>> classes = new ArrayList<>();
        for (int i = 0; i < depth; i++) {
            classes.add((Class<?>) pType.getActualTypeArguments()[i]);
        }
        return classes;
    }

    /**
     * Returns all property descriptors for a given class.
     */
    private List<PropertyDescriptor> getPropertyDescriptorsForClass(Class<?> clasz) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(clasz);
            return Arrays.asList(beanInfo.getPropertyDescriptors());
        } catch (IntrospectionException e) {
            Log.error("instantiation exception while trying to create instance of class {}", clasz.getName());
        }
        return Lists.newArrayList();
    }

    /**
     * Extracts the required values to lookup a model class from the given
     * EDBObject. If this object does not contain the required information, an
     * IllegalArgumentException is thrown.
     */
    private ModelDescription getDescriptionFromObject(Resource obj) {
        Model model = obj.getModel();

        Property hasModel = model.getProperty(JenaConstants.CDL_HAS_MODEL);
        Property modelType = model.getProperty(JenaConstants.CDL_MODEL_TYPE);
        Property modelTypeVersion = model.getProperty(JenaConstants.CDL_MODEL_TYPE_VERSION);

        Statement stmt = obj.getProperty(hasModel);
        RDFNode object = stmt.getObject();
        if (!object.isResource()) {
            throw new JenaException("The object " + obj.getLocalName() + " do not contain model information");
        }
        Resource cdlModel = object.asResource();

        String modelName = cdlModel.getProperty(modelType).getObject().asLiteral().getString();
        String modelVersion = cdlModel.getProperty(modelTypeVersion).getObject().asLiteral().getString();

        if (modelName == null || modelVersion == null) {
            throw new IllegalArgumentException("The object " + obj.getLocalName() + " contains no model information");
        }
        return new ModelDescription(modelName, modelVersion);
    }

    public void setModelRegistry(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

	@SuppressWarnings("unused")
	@Override
	public <T> List<T> query(Query query) {
		List<T> res = new ArrayList<>();
		Log.info("query1");
		
		List<Class<T>> classes = query.getJoinClasses();
		QueryFilter q = query.getFilter();
		List<QueryProjection> projections = query.getProjection();
		Log.info("query1.1");
		
		if(q.getQueryFilterElement() instanceof QueryRequest && !classes.isEmpty()) {
			Log.info("query2");
			Class<T> cls = classes.get(0);
			QueryRequest qr = (QueryRequest) q.getQueryFilterElement();
			List<T> tempRes = singleModelQuery(cls, qr);
			Log.info("query2.1");
			if(tempRes != null) {
				res.addAll(tempRes);
			}
		} else {
			// TODO: handle other type of query
		}
		
		return res;
	}
	
	private <T> List<T> singleModelQuery(Class<T> oesbModel, QueryRequest qr) {
        List<T> oesbModels = new ArrayList<>();
		Log.info("singleModelQuery1");
		
		ParameterizedSparqlString str = JenaQueryRequestConverter.convertSimpleQueryRequest(qr, oesbModel);
		
		if(str!=null) {
			Log.info("singleModelQuery2", str.toString());
			
			dataset.begin(ReadWrite.READ);
			Model model = dataset.getDefaultModel();
			OwlHelper.save(model, "src/test/resources/test-query.owl");
			str.setNsPrefixes(model.getNsPrefixMap());
			QueryExecution exec = QueryExecutionFactory.create(str.toString(), model);
			ResultSet rs = exec.execSelect();
			
			while(rs.hasNext()) {
				Log.info("singleModelQuery2.1");
				QuerySolution solution = rs.next();
				Resource resObj = solution.getResource(oesbModel.getSimpleName());
				T instance = convertResourceToModel(oesbModel, resObj);
				if(instance!=null) {
					Log.info("singleModelQuery2.2");
					oesbModels.add(instance);
				}
			}
			dataset.close();
		}

        return oesbModels;
	}
	
    public <T> List<T> convertResourceToModelObjects(Class<T> model, List<Resource> objects) {
        List<T> models = new ArrayList<>();
        for (Resource object : objects) {
            T instance = convertResourceToModel(model, object);
            if (instance != null) {
                models.add(instance);
            }
        }
        return models;
    }
	
    @SuppressWarnings("unchecked")
    public <T> T convertResourceToModel(Class<T> model, Resource object) {
		Log.info("convertResourceToModel1");
        return (T) convertResourceToUncheckedModel(model, object);
    }
}
