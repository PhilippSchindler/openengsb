/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openengsb.framework.ekb.persistence.orientdb.models;

import org.openengsb.core.api.model.annotation.Model;
import org.openengsb.core.api.model.annotation.OpenEngSBModelId;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Model
public class ComplexModel {

    @OpenEngSBModelId
    private String uiid;

    private String RID;

    // native types
    private String someString;
    private byte someByte;
    private short someShort;
    private int someInteger;
    private long someLong;
    private float someFloat;
    private double someDouble;
    private BigDecimal someDecimal;
    private boolean someBoolean;
    private Date someDate;
    private byte[] someBinary;

    // embedded types (Objects must be convertable to ODocument
    // e.g. the must be simple types or List/Set/Map<String,?> or models
    // embedded objects must not have a RID or uiid, the are only accessable via the outer model
    private Object someEmbeddedObject;
    private List<Object> someEmbeddedList;
    private Set<Object> someEmbeddedSet;
    private Map<String, Object> someEmbeddedMap;

    // custom types
    // private OSerializableStream someStream;

    // link types are handled via relationships


    public String getUiid() {
        return uiid;
    }

    public void setUiid(String uiid) {
        this.uiid = uiid;
    }

    public String getRID() {
        return RID;
    }

    public void setRID(String RID) {
        this.RID = RID;
    }

    public String getSomeString() {
        return someString;
    }

    public void setSomeString(String someString) {
        this.someString = someString;
    }

    public byte getSomeByte() {
        return someByte;
    }

    public void setSomeByte(byte someByte) {
        this.someByte = someByte;
    }

    public short getSomeShort() {
        return someShort;
    }

    public void setSomeShort(short someShort) {
        this.someShort = someShort;
    }

    public int getSomeInteger() {
        return someInteger;
    }

    public void setSomeInteger(int someInteger) {
        this.someInteger = someInteger;
    }

    public long getSomeLong() {
        return someLong;
    }

    public void setSomeLong(long someLong) {
        this.someLong = someLong;
    }

    public float getSomeFloat() {
        return someFloat;
    }

    public void setSomeFloat(float someFloat) {
        this.someFloat = someFloat;
    }

    public double getSomeDouble() {
        return someDouble;
    }

    public void setSomeDouble(double someDouble) {
        this.someDouble = someDouble;
    }

    public BigDecimal getSomeDecimal() {
        return someDecimal;
    }

    public void setSomeDecimal(BigDecimal someDecimal) {
        this.someDecimal = someDecimal;
    }

    public boolean getSomeBoolean() {
        return someBoolean;
    }

    public void setSomeBoolean(boolean someBoolean) {
        this.someBoolean = someBoolean;
    }

    public Date getSomeDate() {
        return someDate;
    }

    public void setSomeDate(Date someDate) {
        this.someDate = someDate;
    }

    public byte[] getSomeBinary() {
        return someBinary;
    }

    public void setSomeBinary(byte[] someBinary) {
        this.someBinary = someBinary;
    }

    public Object getSomeEmbeddedObject() {
        return someEmbeddedObject;
    }

    public void setSomeEmbeddedObject(Object someEmbeddedObject) {
        this.someEmbeddedObject = someEmbeddedObject;
    }

    public List<Object> getSomeEmbeddedList() {
        return someEmbeddedList;
    }

    public void setSomeEmbeddedList(List<Object> someEmbeddedList) {
        this.someEmbeddedList = someEmbeddedList;
    }

    public Set<Object> getSomeEmbeddedSet() {
        return someEmbeddedSet;
    }

    public void setSomeEmbeddedSet(Set<Object> someEmbeddedSet) {
        this.someEmbeddedSet = someEmbeddedSet;
    }

    public Map<String, Object> getSomeEmbeddedMap() {
        return someEmbeddedMap;
    }

    public void setSomeEmbeddedMap(Map<String, Object> someEmbeddedMap) {
        this.someEmbeddedMap = someEmbeddedMap;
    }
}
