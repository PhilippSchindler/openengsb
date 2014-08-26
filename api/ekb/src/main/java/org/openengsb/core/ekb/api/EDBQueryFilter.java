package org.openengsb.core.ekb.api;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.openengsb.core.api.model.QueryRequest;
import org.openengsb.core.ekb.api.internal.OntoQueryParser;

public class EDBQueryFilter implements QueryFilter {
    protected QueryRequest queryRequest;
    protected List<QueryParser> queryParsers = new ArrayList<QueryParser>();

    public EDBQueryFilter() {
        queryParsers.add(new OntoQueryParser());
        queryRequest = QueryRequest.create();
    }

    public EDBQueryFilter(QueryRequest queryRequest) {
        queryParsers.add(new OntoQueryParser());
        this.queryRequest = queryRequest;
    }

    public EDBQueryFilter(String queryRequest) {
        queryParsers.add(new OntoQueryParser());
        this.queryRequest = parseQueryString(queryRequest);
    }

    public List<QueryParser> getQueryParsers() {
        return queryParsers;
    }

    private QueryRequest parseQueryString(String query) throws EKBException {
        if (query.isEmpty()) {
            return QueryRequest.create();
        }
    	System.out.println("Parsing Started ");
        for (QueryParser parser : queryParsers) {
        	System.out.println("Query: "+query);
            if (parser.isParsingPossible(query)) {
            	System.out.println("Parsing Possible");
                return parser.parseQueryString(query);
            }
        }
    	System.out.println("Parsing impossible");
        throw new EKBException("No active parser which is able to parse the query string " + query);
    }

    @Override
    public boolean filter(Object... objects) {
        return true;
    }

	@Override
	public Object getQueryFilterElement() {
		return queryRequest;
	}

	@Override
	public void setQueryFilterElement(Object object) {
		if(object instanceof QueryRequest) {
			queryRequest = (QueryRequest) object;
		} else if (object instanceof String) {
			try {
				queryRequest = parseQueryString((String)object);
			} catch (EKBException e) {
				// TODO;
				e.printStackTrace();
			}
		}
	}
}
