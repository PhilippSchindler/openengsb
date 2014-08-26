package org.openengsb.core.ekb.persistence.jena.internal;

import java.util.Iterator;
import java.util.Set;

import org.openengsb.core.api.context.ContextHolder;
import org.openengsb.core.api.model.QueryRequest;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class JenaQueryRequestConverter {
	public static ParameterizedSparqlString convertSimpleQueryRequest(QueryRequest request, Class<?> cls) {
		String clsName;
		if(!request.getModelClassName().isEmpty()) {
			clsName = request.getModelClassName(); 
		} else {
			clsName = cls.getSimpleName();
		}

        String context = ContextHolder.get().getCurrentContextId();
		ParameterizedSparqlString sparqlString = new ParameterizedSparqlString();
		
		sparqlString.append("select ?"+clsName+" where { \n");
		sparqlString.append("?context rdf:type :Context . \n");
		sparqlString.append("?context :contextId \""+context+"\" . \n");
		sparqlString.append("?context :hasHeadCommit/:hasEntities ?"+clsName+" . \n");
		sparqlString.append("?"+clsName+" a :"+clsName+" . \n ");
		
		Iterator<String> keyset = request.getParameters().keySet().iterator();
		
		while(keyset.hasNext()) {
			
			String key = keyset.next();
			Set<Object> values = request.getParameter(key);
			Iterator<Object> iterator = values.iterator();
			
			while(iterator.hasNext()) {
				Object value = iterator.next();
				sparqlString.append("?"+clsName+" ");
				sparqlString.append(":"+key+" ?"+key);
				sparqlString.append(" . \n ");
				
				if(value instanceof String && !request.isCaseSensitive()) {
					sparqlString.append(" FILTER (lcase(str(?"+key+")) = \""+value.toString().toLowerCase()+"\")");
				} else {
					RDFNode node = ResourceFactory.createTypedLiteral(value);
					sparqlString.setParam(key, node);
				}
				
				sparqlString.append(" . \n ");
			}
		}
		sparqlString.append(" }");
		
		
		return sparqlString;
	}
}
