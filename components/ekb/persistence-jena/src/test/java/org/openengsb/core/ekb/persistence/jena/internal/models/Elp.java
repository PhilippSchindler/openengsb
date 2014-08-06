// CHECKSTYLE:OFF
package org.openengsb.core.ekb.persistence.jena.internal.models;

import java.io.Serializable;
import java.util.List;

import org.openengsb.core.api.model.annotation.IgnoredModelField;
import org.openengsb.core.api.model.annotation.Model;
import org.openengsb.core.api.model.annotation.OpenEngSBModelId;

/**
 * This class represents a model of an entity of the electrical planning domain.
 */
@Model
public class Elp implements Serializable {
    @IgnoredModelField
    private static final long serialVersionUID = 7137843608768285548L;

    @OpenEngSBModelId
    private String uuid;
    private String project;
    private String region;
    private String componentNumber;
    private String cpuNumber;
    private String channelName;
    private String rackId;
    private String position;
    private String kks0;
    private String kks1;
    private String kks2;
    private String kks3;
    private String longText;
    private String status;
    private String physicalPin;
    private String placement;

    private String sw;
    private List<String> pins;

    // Custom defined fields
    private String custom1;
    private String custom2;
    private String custom3;
    private String custom4;
    private String custom5;
    private String custom6;
    private String custom7;
    private String custom8;
    private String custom9;
    private String custom10;
    private String custom11;
    private String custom12;
    private String custom13;
    private String custom14;
    private String custom15;
    private String custom16;
    private String custom17;
    private String custom18;
    private String custom19;
    private String custom20;
    private String custom21;
    private String custom22;
    private String custom23;
    private String custom24;
    private String custom25;
    private String custom26;
    private String custom27;
    private String custom28;
    private String custom29;
    private String custom30;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getComponentNumber() {
        return componentNumber;
    }

    public void setComponentNumber(String componentNumber) {
        this.componentNumber = componentNumber;
    }

    public String getCpuNumber() {
        return cpuNumber;
    }

    public void setCpuNumber(String cpuNumber) {
        this.cpuNumber = cpuNumber;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getRackId() {
        return rackId;
    }

    public void setRackId(String rackId) {
        this.rackId = rackId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
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

    public String getLongText() {
        return longText;
    }

    public void setLongText(String longText) {
        this.longText = longText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getSw() {
        return sw;
    }

    public void setSw(String sw) {
        this.sw = sw;
    }

    public List<String> getPins() {
        return pins;
    }

    public void setPins(List<String> pins) {
        this.pins = pins;
    }

    public String getPhysicalPin() {
        return physicalPin;
    }

    public void setPhysicalPin(String physicalPin) {
        this.physicalPin = physicalPin;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public String getCustom1() {
        return custom1;
    }

    public void setCustom1(String custom1) {
        this.custom1 = custom1;
    }

    public String getCustom2() {
        return custom2;
    }

    public void setCustom2(String custom2) {
        this.custom2 = custom2;
    }

    public String getCustom3() {
        return custom3;
    }

    public void setCustom3(String custom3) {
        this.custom3 = custom3;
    }

    public String getCustom4() {
        return custom4;
    }

    public void setCustom4(String custom4) {
        this.custom4 = custom4;
    }

    public String getCustom5() {
        return custom5;
    }

    public void setCustom5(String custom5) {
        this.custom5 = custom5;
    }

    public String getCustom6() {
        return custom6;
    }

    public void setCustom6(String custom6) {
        this.custom6 = custom6;
    }

    public String getCustom7() {
        return custom7;
    }

    public void setCustom7(String custom7) {
        this.custom7 = custom7;
    }

    public String getCustom8() {
        return custom8;
    }

    public void setCustom8(String custom8) {
        this.custom8 = custom8;
    }

    public String getCustom9() {
        return custom9;
    }

    public void setCustom9(String custom9) {
        this.custom9 = custom9;
    }

    public String getCustom10() {
        return custom10;
    }

    public void setCustom10(String custom10) {
        this.custom10 = custom10;
    }

    public String getCustom11() {
        return custom11;
    }

    public void setCustom11(String custom11) {
        this.custom11 = custom11;
    }

    public String getCustom12() {
        return custom12;
    }

    public void setCustom12(String custom12) {
        this.custom12 = custom12;
    }

    public String getCustom13() {
        return custom13;
    }

    public void setCustom13(String custom13) {
        this.custom13 = custom13;
    }

    public String getCustom14() {
        return custom14;
    }

    public void setCustom14(String custom14) {
        this.custom14 = custom14;
    }

    public String getCustom15() {
        return custom15;
    }

    public void setCustom15(String custom15) {
        this.custom15 = custom15;
    }

    public String getCustom16() {
        return custom16;
    }

    public void setCustom16(String custom16) {
        this.custom16 = custom16;
    }

    public String getCustom17() {
        return custom17;
    }

    public void setCustom17(String custom17) {
        this.custom17 = custom17;
    }

    public String getCustom18() {
        return custom18;
    }

    public void setCustom18(String custom18) {
        this.custom18 = custom18;
    }

    public String getCustom19() {
        return custom19;
    }

    public void setCustom19(String custom19) {
        this.custom19 = custom19;
    }

    public String getCustom20() {
        return custom20;
    }

    public void setCustom20(String custom20) {
        this.custom20 = custom20;
    }

    public String getCustom21() {
        return custom21;
    }

    public void setCustom21(String custom21) {
        this.custom21 = custom21;
    }

    public String getCustom22() {
        return custom22;
    }

    public void setCustom22(String custom22) {
        this.custom22 = custom22;
    }

    public String getCustom23() {
        return custom23;
    }

    public void setCustom23(String custom23) {
        this.custom23 = custom23;
    }

    public String getCustom24() {
        return custom24;
    }

    public void setCustom24(String custom24) {
        this.custom24 = custom24;
    }

    public String getCustom25() {
        return custom25;
    }

    public void setCustom25(String custom25) {
        this.custom25 = custom25;
    }

    public String getCustom26() {
        return custom26;
    }

    public void setCustom26(String custom26) {
        this.custom26 = custom26;
    }

    public String getCustom27() {
        return custom27;
    }

    public void setCustom27(String custom27) {
        this.custom27 = custom27;
    }

    public String getCustom28() {
        return custom28;
    }

    public void setCustom28(String custom28) {
        this.custom28 = custom28;
    }

    public String getCustom29() {
        return custom29;
    }

    public void setCustom29(String custom29) {
        this.custom29 = custom29;
    }

    public String getCustom30() {
        return custom30;
    }

    public void setCustom30(String custom30) {
        this.custom30 = custom30;
    }
}
