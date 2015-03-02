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

package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import java.io.IOException;

public class OrientDBHelper {

    private String databaseName;
    private String user;
    private String password;
    private String storageType;
    private String connectionURL;

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public void setConnectionURL(String connectionURL) {
        this.connectionURL = connectionURL;
    }

    private static OrientDBHelper defaultHelper;

    public static OrientDBHelper getDefault() {
        if (defaultHelper == null) {
            defaultHelper = getEmbedded();
        }
        return defaultHelper;
    }

    private static OrientDBHelper embeddedHelper;

    public static OrientDBHelper getEmbedded() {
        if (embeddedHelper == null) {
            OrientDBHelper helper = new OrientDBHelper();
            helper.setDatabaseName("project-config-with-versioning");
            helper.setUser("admin");
            helper.setPassword("admin");
            helper.setStorageType("plocal");
            helper.setConnectionURL("plocal:F:\\orientdb-embedded\\");
            embeddedHelper = helper;
            return helper;
        }
        return embeddedHelper;
    }

    private static OrientDBHelper remoteHelper;

    public static OrientDBHelper getRemote() {
        if (remoteHelper == null) {
            OrientDBHelper helper = new OrientDBHelper();
            helper.setDatabaseName("project-config-with-versioning");
            helper.setUser("admin");
            helper.setPassword("admin");
            helper.setStorageType("plocal");
            helper.setConnectionURL("remote:localhost");
            remoteHelper = helper;
            return helper;
        }
        return remoteHelper;
    }

    private static OrientDBHelper inMemoryHelper;

    public static OrientDBHelper getInMemory() {
        if (inMemoryHelper == null) {
            OrientDBHelper helper = new OrientDBHelper();
            helper.setDatabaseName("project-config-with-versioning");
            helper.setUser("admin");
            helper.setPassword("admin");
            helper.setStorageType("memory");
            helper.setConnectionURL("memory:/temp/db");
            inMemoryHelper = helper;
            return helper;
        }
        return inMemoryHelper;
    }

    public void createOrOverwriteDatabase() throws IOException {
        if (connectionURL.startsWith("remote")) {
            OServerAdmin admin = new OServerAdmin(connectionURL + "/" + databaseName).connect(user, password);
            if (admin.existsDatabase()) {
                admin.dropDatabase(storageType);
            }
            admin.createDatabase(databaseName, "document", storageType);
            admin.close();
            ODatabaseDocumentTx database = new ODatabaseDocumentTx(connectionURL + "/" + databaseName);
            database.open(user, password);
            database.close();
        } else {
            ODatabaseDocumentTx database = new ODatabaseDocumentTx(connectionURL + "/" + databaseName);
            if (database.exists()) {
                database.open(user, password);
                database.drop();
                database = new ODatabaseDocumentTx(connectionURL + "/" + databaseName);
            }
            database.create();
        }
    }

    public ODatabaseDocumentTx getConnection() {
        return new ODatabaseDocumentTx(connectionURL + "/" + databaseName).open(user, password);
    }

    public ODatabaseDocument getConnectionNoTx() {
        return getConnection();
    }

}
