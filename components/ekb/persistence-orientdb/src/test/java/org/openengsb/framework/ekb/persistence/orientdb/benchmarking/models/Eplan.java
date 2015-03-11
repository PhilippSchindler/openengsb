package org.openengsb.framework.ekb.persistence.orientdb.benchmarking.models;

import org.openengsb.core.api.model.annotation.Model;

@Model
public class Eplan {

    private String rid;
    private String func_text;
    private String address;
    private String signal_number;

    public Eplan() {
    }

    public Eplan(String rid, String func_text, String address, String signal_number) {
        super();
        this.rid = rid;
        this.func_text = func_text;
        this.address = address;
        this.signal_number = signal_number;
    }

    public void setAttributes(String rid, String func_text, String address, String signal_number) {
        this.rid = rid;
        this.func_text = func_text;
        this.address = address;
        this.signal_number = signal_number;
    }

    public void setEmpty() {
        this.rid = "";
        this.func_text = "";
        this.address = "";
        this.signal_number = "";
    }

    public String getRID() {
        return rid;
    }

    public void setRID(String rid) {
        this.rid = rid;
    }

    public String getFunc_text() {
        return func_text;
    }

    public void setFunc_text(String func_text) {
        this.func_text = func_text;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSignal_number() {
        return signal_number;
    }

    public void setSignal_number(String signal_number) {
        this.signal_number = signal_number;
    }

}
