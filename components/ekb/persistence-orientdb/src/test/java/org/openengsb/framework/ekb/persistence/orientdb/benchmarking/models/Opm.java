package org.openengsb.framework.ekb.persistence.orientdb.benchmarking.models;

import org.openengsb.core.api.model.annotation.Model;

@Model
public class Opm {

    private String rid;
    private String GRP;
    private String SYS;
    private String EQP;
    private String SIG;
    private String long_text;
    private String KOMP;
    private String BSE;
    private String SSE;

    public Opm() {
    }

    public Opm(String rid, String gRP, String sYS, String eQP, String sIG,
            String long_text, String kOMP, String bSE, String sSE) {
        super();
        this.rid = rid;
        this.GRP = gRP;
        this.SYS = sYS;
        this.EQP = eQP;
        this.SIG = sIG;
        this.long_text = long_text;
        this.KOMP = kOMP;
        this.BSE = bSE;
        this.SSE = sSE;
    }

    public Opm(Vcdm vcdm) {
        super();
        this.GRP = vcdm.getKks0();
        this.SYS = vcdm.getKks1();
        this.EQP = vcdm.getKks2();
        this.SIG = vcdm.getKks3();
        this.long_text = vcdm.getLong_text();
        this.KOMP = vcdm.getComp_number();
        this.BSE = vcdm.getCpu_number();
        this.SSE = vcdm.getRack_id();
    }

    public void setAttributes(Vcdm vcdm) {
        this.GRP = vcdm.getKks0();
        this.SYS = vcdm.getKks1();
        this.EQP = vcdm.getKks2();
        this.SIG = vcdm.getKks3();
        this.long_text = vcdm.getLong_text();
        this.KOMP = vcdm.getComp_number();
        this.BSE = vcdm.getCpu_number();
        this.SSE = vcdm.getRack_id();
    }

    public void setEmpty() {
        this.rid = "";
        this.GRP = "";
        this.SYS = "";
        this.EQP = "";
        this.SIG = "";
        this.long_text = "";
        this.KOMP = "";
        this.BSE = "";
        this.SSE = "";
    }

    public String getRID() {
        return rid;
    }

    public void setRID(String rid) {
        this.rid = rid;
    }

    public String getGRP() {
        return GRP;
    }

    public void setGRP(String gRP) {
        GRP = gRP;
    }

    public String getSYS() {
        return SYS;
    }

    public void setSYS(String sYS) {
        SYS = sYS;
    }

    public String getEQP() {
        return EQP;
    }

    public void setEQP(String eQP) {
        EQP = eQP;
    }

    public String getSIG() {
        return SIG;
    }

    public void setSIG(String sIG) {
        SIG = sIG;
    }

    public String getLong_text() {
        return long_text;
    }

    public void setLong_text(String long_text) {
        this.long_text = long_text;
    }

    public String getKOMP() {
        return KOMP;
    }

    public void setKOMP(String kOMP) {
        KOMP = kOMP;
    }

    public String getBSE() {
        return BSE;
    }

    public void setBSE(String bSE) {
        BSE = bSE;
    }

    public String getSSE() {
        return SSE;
    }

    public void setSSE(String sSE) {
        SSE = sSE;
    }
}
