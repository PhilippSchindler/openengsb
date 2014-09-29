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

package org.openengsb.framework.ekb.persistence.orientdb.visualization;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class DotExporter {

    public static void export(ODatabaseDocumentTx database, String filepath, String... classes) {

        StringBuilder sb = new StringBuilder();
        String svgpath = filepath.split("\\.")[0] + ".svg";

        sb.append("digraph {\n");
        sb.append("overlap = false;\n");
        sb.append("sep = \"+20\";\n");
        sb.append("splines=true;\n");
        sb.append("epsilon=0.000001;\n");

        if (classes.length == 0)
            classes = new String[]{ "V" };

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getDotNode(ODocument doc) {
        return "\"" + doc.getClassName() + " " + doc.getIdentity().toString() + "\"";
    }

}
