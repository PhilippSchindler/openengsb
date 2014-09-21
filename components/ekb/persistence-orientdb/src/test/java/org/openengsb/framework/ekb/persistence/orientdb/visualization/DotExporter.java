package org.openengsb.framework.ekb.persistence.orientdb.visualization;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Created by Philipp Schindler on 21.09.2014.
 */
public class DotExporter {

    public static void export(ODatabaseDocumentTx database, String filepath, String ...classes) {

        StringBuilder sb = new StringBuilder();
        String svgpath = filepath.split("\\.")[0] + ".svg";

        sb.append("digraph {\n");
        sb.append("overlap = false;\n");
        sb.append("sep = \"+20\";\n");
        sb.append("splines=true;\n");
        sb.append("epsilon=0.000001;\n");


        for (String clazz : classes) {
            for (ODocument doc : database.browseClass(clazz)) {
                sb.append(getDotNode(doc) + "\n");
            }
        }

        sb.append("\n");

        for (String clazz : classes) {
            for (ODocument doc : database.browseClass(clazz)) {
                String rid = doc.getIdentity().toString();
                for (String fieldName : doc.fieldNames()) {
                    Object field = doc.field(fieldName);
                    if (field instanceof ODocument) {
                        sb.append(getDotNode(doc) + " -> " + getDotNode((ODocument) field) +
                               " [ label=" + fieldName + " ];\n");
                    } else if (field instanceof List<?>) {
                        for (Object o : (List<?>) field) {
                            if (o instanceof ODocument) {
                                sb.append(getDotNode(doc) + " -> " + getDotNode((ODocument) o) +
                                        " [ label=" + fieldName + " ];\n");
                            }
                        }
                    }
                }
            }
        }

        sb.append("}\n");

        try {
            PrintWriter out = new PrintWriter(filepath);
            out.println(sb.toString());
            out.close();
            Runtime.getRuntime().exec("neato -Tsvg \"" + filepath + "\" -o \"" + svgpath + "\"");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getDotNode(ODocument doc) {
        return "\"" + doc.getClassName() + " " + doc.getIdentity().toString() + "\"";
    }

}
