import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import org.openengsb.framework.ekb.persistence.orientdb.OrientDBHelper;
import org.openengsb.framework.ekb.persistence.orientdb.SchemaGenerator;
import org.openengsb.framework.ekb.persistence.orientdb.models.Activity;
import org.openengsb.framework.ekb.persistence.orientdb.models.Manager;
import org.openengsb.framework.ekb.persistence.orientdb.models.Person;
import org.openengsb.framework.ekb.persistence.orientdb.models.Project;

import java.io.IOException;

/**
 * Created by Philipp Schindler on 13.09.2014.
 */
public class Prototyping {
    public static void main(String[] args) throws IOException {

        OrientDBHelper.getDefault().createOrOverwriteDatabase();
        OrientGraphNoTx database = OrientDBHelper.getDefault().getConnectionNoTx();

        SchemaGenerator generator = new SchemaGenerator();
        generator.setDatabase(database);
        generator.addModel(Activity.class);
        generator.addModel(Project.class);
        generator.addModel(Person.class);
        generator.addModel(Manager.class);  // manager must be added after person due to inheritance

        generator.generateVersioningSchema();
        generator.generateSchemaForModels();
    }
}