package org.openengsb.framework.ekb.persistence.orientdb.benchmarking;

import org.openengsb.framework.ekb.persistence.orientdb.EKBCommit;
import org.openengsb.framework.ekb.persistence.orientdb.EKBCommitImpl;
import org.openengsb.framework.ekb.persistence.orientdb.Operation;
import org.openengsb.framework.ekb.persistence.orientdb.OperationType;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

/**
 * Created by sp on 02.03.2015.
 */
public class ScenarioLoader {

    public static EKBCommit loadCommit(String baseFolderPath, int scenarioNumber, int commitNumber)
            throws IOException
    {
        EKBCommit commit = new EKBCommitImpl();

        String insertsCsv = Paths.get(baseFolderPath, "scenario_" + scenarioNumber,
                String.format("scenario_%d_commit_%d_inserts.csv", scenarioNumber, commitNumber)).toString();

        String deletesCsv = Paths.get(baseFolderPath, "scenario_" + scenarioNumber,
                String.format("scenario_%d_commit_%d_deletes.csv", scenarioNumber, commitNumber)).toString();

        String updatesCsv = Paths.get(baseFolderPath, "scenario_" + scenarioNumber,
                String.format("scenario_%d_commit_%d_updates.csv", scenarioNumber, commitNumber)).toString();

        addCommitOperations(insertsCsv, OperationType.INSERT, commit);
        addCommitOperations(deletesCsv, OperationType.DELETE, commit);
        addCommitOperations(updatesCsv, OperationType.UPDATE, commit);

        return commit;
    }

    private static void addCommitOperations(String insertUpdateDeleteFilepath, OperationType operationType,
            EKBCommit commit) throws IOException
    {
        FileInputStream fstream = new FileInputStream(insertUpdateDeleteFilepath);
        BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

        String line = br.readLine(); // ignore header of csv file
        while ((line = br.readLine()) != null)
        {
            String[] parts = line.split(";");

            Signal signal = new Signal();
            signal.setRID("#14:" + parts[0]);
            signal.setSigNr(parts[1]);
            signal.setFuncText(parts[2]);
            signal.setAddress(parts[3]);

            commit.addOperation(new Operation(operationType, signal));
        }

        br.close();
    }

}
