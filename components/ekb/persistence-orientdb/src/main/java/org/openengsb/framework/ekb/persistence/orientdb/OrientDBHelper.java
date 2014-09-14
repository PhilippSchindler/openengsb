package org.openengsb.framework.ekb.persistence.orientdb;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

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
            OrientDBHelper helper = new OrientDBHelper();
            helper.setDatabaseName("project-config-with-versioning");
            helper.setUser("admin");
            helper.setPassword("12345");
            helper.setStorageType("plocal");
            helper.setConnectionURL("remote:localhost");
            defaultHelper = helper;
            return helper;
        }
        return defaultHelper;
    }

    public void createOrOverwriteDatabase() throws IOException {
        OServerAdmin admin = new OServerAdmin(connectionURL + "/" + databaseName).connect(user, password);

        if (admin.existsDatabase())
            admin.dropDatabase(storageType);

        admin.createDatabase(databaseName, "graph", storageType);
        admin.close();

        ODatabaseDocumentTx database = new ODatabaseDocumentTx(connectionURL + "/" + databaseName);
        database.open(user, password);
        database.close();
    }

    public OrientGraph getConnection() {
        return new OrientGraph(connectionURL + "/" + databaseName, user, password);
    }

    public OrientGraphNoTx getConnectionNoTx() {
        return new OrientGraphNoTx(connectionURL + "/" + databaseName, user, password);
    }



}
