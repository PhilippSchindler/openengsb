package org.openengsb.core.ekb.persistence.onto.internal;

import org.openengsb.core.api.model.OpenEngSBModel;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class OntoToOESB {
    public static Resource convertOESBToResource(OpenEngSBModel model) {
        return null;
    }

    public static OpenEngSBModel convertResourceToOESB(Resource resource) {
        StmtIterator propStmts = resource.listProperties();
        while (propStmts.hasNext()) {
            Statement stmt = propStmts.next();
            RDFNode obj = stmt.getObject();
            OpenEngSBModel oesbmodel = (OpenEngSBModel) obj.visitWith(new OESBVisitor());
        }
        return null;
    }
}
