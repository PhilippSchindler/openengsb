package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import java.io.IOException;

/**
 * Created by Philipp Schindler on 14.09.2014.
 */
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
            defaultHelper = getInMemory();
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
            helper.setConnectionURL("plocal:/temp/db");
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
            if (admin.existsDatabase())
                admin.dropDatabase(storageType);
            admin.createDatabase(databaseName, "document", storageType);
            admin.close();
            ODatabaseDocumentTx database = new ODatabaseDocumentTx(connectionURL + "/" + databaseName);
            database.open(user, password);
            database.close();
        }
        else {
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
