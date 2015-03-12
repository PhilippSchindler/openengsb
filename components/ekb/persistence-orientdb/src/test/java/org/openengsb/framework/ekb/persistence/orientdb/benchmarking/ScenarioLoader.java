package org.openengsb.framework.ekb.persistence.orientdb.benchmarking;

import org.openengsb.framework.ekb.persistence.orientdb.EKBCommit;
import org.openengsb.framework.ekb.persistence.orientdb.EKBCommitImpl;
import org.openengsb.framework.ekb.persistence.orientdb.Operation;
import org.openengsb.framework.ekb.persistence.orientdb.OperationType;
import org.openengsb.framework.ekb.persistence.orientdb.benchmarking.models.Eplan;
import org.openengsb.framework.ekb.persistence.orientdb.benchmarking.models.Opm;
import org.openengsb.framework.ekb.persistence.orientdb.benchmarking.models.Vcdm;

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
    {
        EKBCommit commit = new EKBCommitImpl();

        String insertsCsv = Paths.get(baseFolderPath, "scenario_" + scenarioNumber,
                String.format("scenario_%d_commit_%d_inserts.csv", scenarioNumber, commitNumber)).toString();

        String deletesCsv = Paths.get(baseFolderPath, "scenario_" + scenarioNumber,
                String.format("scenario_%d_commit_%d_deletes.csv", scenarioNumber, commitNumber)).toString();

        String updatesCsv = Paths.get(baseFolderPath, "scenario_" + scenarioNumber,
                String.format("scenario_%d_commit_%d_updates.csv", scenarioNumber, commitNumber)).toString();

        try {
            addCommitOperations(insertsCsv, OperationType.INSERT, commit);
            addCommitOperations(deletesCsv, OperationType.DELETE, commit);
            addCommitOperations(updatesCsv, OperationType.UPDATE, commit);
        } catch (IOException e) {
            return null;
        }

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

            Eplan eplan = new Eplan();
            eplan.setRID(Benchmarking.EPLAN_ID_PREFIX + parts[0]);
            eplan.setSignal_number(parts[1]);
            eplan.setFunc_text(parts[2]);
            eplan.setAddress(parts[3]);

            Vcdm vcdm = new Vcdm(eplan);
            // set record id based on the id of eplan - change orientdb cluster by 3 (gives cluster of vcdm)
            vcdm.setRID(Benchmarking.VCDM_ID_PREFIX + parts[0]);

            Opm opm = new Opm(vcdm);
            // set record id based on the id of eplan - change orientdb cluster by 6 (gives cluster of opm)
            opm.setRID(Benchmarking.OPM_ID_PREFIX + parts[0]);

            commit.addOperation(new Operation(operationType, eplan));
            commit.addOperation(new Operation(operationType, vcdm));
            commit.addOperation(new Operation(operationType, opm));
        }

        br.close();
    }

}
