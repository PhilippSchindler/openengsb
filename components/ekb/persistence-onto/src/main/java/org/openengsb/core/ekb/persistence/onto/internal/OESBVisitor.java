package org.openengsb.core.ekb.persistence.onto.internal;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFVisitor;
import com.hp.hpl.jena.rdf.model.Resource;

public class OESBVisitor implements RDFVisitor {

    @Override
    public Object visitBlank(Resource r, AnonId id) {
        return null;
    }

    @Override
    public Object visitURI(Resource r, String uri) {
        return OntoToOESB.convertResourceToOESB(r);
    }

    @Override
    public Object visitLiteral(Literal l) {
        Object returnValue = null;
        if (l.getDatatype().equals(XSDDatatype.XSDint) || l.getDatatype().equals(XSDDatatype.XSDinteger)) {
            Integer integer = l.getInt();
            returnValue = integer;
        } else if (l.getDatatype().equals(XSDDatatype.XSDstring)) {
            returnValue = l.getString();
        } else {
            returnValue = l.getLexicalForm().toString();
        }
        return returnValue;
    }

}
