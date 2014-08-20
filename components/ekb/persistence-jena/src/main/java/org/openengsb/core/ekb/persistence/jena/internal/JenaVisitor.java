package org.openengsb.core.ekb.persistence.jena.internal;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFVisitor;
import com.hp.hpl.jena.rdf.model.Resource;

public class JenaVisitor implements RDFVisitor {

    @Override
    public Object visitBlank(Resource r, AnonId id) {
        return null;
    }

    @Override
    public Object visitURI(Resource r, String uri) {
        return r.getLocalName();
    }

    @Override
    public Object visitLiteral(Literal l) {
        RDFDatatype datatype = l.getDatatype();
        Object obj = l.getValue();
        if (datatype != null) {
            if (datatype.equals(XSDDatatype.XSDdateTime)) {
                XSDDateTime time = (XSDDateTime) l.getValue();
                obj = time.asCalendar();
            } else {
                obj = l.getLexicalForm();
            }
        }
        return obj;
    }
}
