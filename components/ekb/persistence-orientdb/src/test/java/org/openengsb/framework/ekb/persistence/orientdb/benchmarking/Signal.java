package org.openengsb.framework.ekb.persistence.orientdb.benchmarking;

import org.openengsb.core.api.model.annotation.Model;

@Model
public class Signal {

    private String rid;
    private String sigNr;
    private String funcText;
    private String address;

    public String getRID() {
        return rid;
    }

    public void setRID(String RID) {
        this.rid = RID;
    }

    public String getSigNr() {
        return sigNr;
    }

    public void setSigNr(String sigNr) {
        this.sigNr = sigNr;
    }

    public String getFuncText() {
        return funcText;
    }

    public void setFuncText(String funcText) {
        this.funcText = funcText;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
