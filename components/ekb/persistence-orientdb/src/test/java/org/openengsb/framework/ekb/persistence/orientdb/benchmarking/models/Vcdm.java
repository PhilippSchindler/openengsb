package org.openengsb.framework.ekb.persistence.orientdb.benchmarking.models;

import org.openengsb.core.api.model.annotation.Model;

@Model
public class Vcdm
{
    private String rid;
    private String kks0;
    private String kks1;
    private String kks2;
    private String kks3;
    private String long_text;
    private String comp_number;
    private String cpu_number;
    private String rack_id;

    public Vcdm() {
    }

    public Vcdm(String rid, String kks0, String kks1, String kks2,
            String kks3, String long_text, String comp_number,
            String cpu_number, String rack_id) {
        super();
        this.rid = rid;
        this.kks0 = kks0;
        this.kks1 = kks1;
        this.kks2 = kks2;
        this.kks3 = kks3;
        this.long_text = long_text;
        this.comp_number = comp_number;
        this.cpu_number = cpu_number;
        this.rack_id = rack_id;
    }

    public Vcdm(Eplan eplan) {
        this.kks0 = eplan.getSignal_number().substring(0,2);
        this.kks1 = eplan.getSignal_number().substring(3,8);
        this.kks2 = eplan.getSignal_number().substring(9,14);
        this.kks3 = eplan.getSignal_number().substring(15,19);
        this.long_text = eplan.getFunc_text();
        this.comp_number = eplan.getAddress().substring(0,3);
        this.cpu_number = eplan.getAddress().substring(4,6);
        this.rack_id = eplan.getAddress().substring(7,9);
    }

    public void setAttributes(Eplan eplan) {
        this.kks0 = eplan.getSignal_number().substring(0,2);
        this.kks1 = eplan.getSignal_number().substring(3,8);
        this.kks2 = eplan.getSignal_number().substring(9, 14);
        this.kks3 = eplan.getSignal_number().substring(15,19);
        this.long_text = eplan.getFunc_text();
        this.comp_number = eplan.getAddress().substring(0,3);
        this.cpu_number = eplan.getAddress().substring(4, 6);
        this.rack_id = eplan.getAddress().substring(7, 9);
    }

    public void setEmpty() {
        this.rid = "";
        this.kks0 = "";
        this.kks1 = "";
        this.kks2 = "";
        this.kks3 = "";
        this.long_text = "";
        this.comp_number = "";
        this.cpu_number = "";
        this.rack_id = "";
    }

    public String getRID() {
        return rid;
    }

    public void setRID(String rid) {
        this.rid = rid;
    }

    public String getKks0() {
        return kks0;
    }

    public void setKks0(String kks0) {
        this.kks0 = kks0;
    }


    public String getKks1() {
        return kks1;
    }

    public void setKks1(String kks1) {
        this.kks1 = kks1;
    }


    public String getKks2() {
        return kks2;
    }

    public void setKks2(String kks2) {
        this.kks2 = kks2;
    }


    public String getKks3() {
        return kks3;
    }

    public void setKks3(String kks3) {
        this.kks3 = kks3;
    }


    public String getLong_text() {
        return long_text;
    }

    public void setLong_text(String long_text) {
        this.long_text = long_text;
    }


    public String getComp_number() {
        return comp_number;
    }

    public void setComp_number(String comp_number) {
        this.comp_number = comp_number;
    }


    public String getCpu_number() {
        return cpu_number;
    }

    public void setCpu_number(String cpu_number) {
        this.cpu_number = cpu_number;
    }


    public String getRack_id() {
        return rack_id;
    }

    public void setRack_id(String rack_id) {
        this.rack_id = rack_id;
    }

}




