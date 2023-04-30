package org.matchers;

import au.com.bytecode.opencsv.CSVReader;
import org.converter.Node;
import org.utils.Correspondence;
import org.python.util.PythonInterpreter;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for XGBoost Python schema matcher provided by GitHub user fireindark707.
 */
public class XGBoostWrapper implements MatcherWrapperInterface {
    private final String m_pythonSourcePath;
    private final String m_csvOutputPath;
    public XGBoostWrapper(String pythonSourcePath, String csvOutputPath) {
        m_pythonSourcePath = pythonSourcePath;
        m_csvOutputPath = csvOutputPath;
    }

    public void execute() {
        //todo: jython import krampf
        final var interpreter = new PythonInterpreter();

        // train xgboost model (only required once)
        //interpreter.execfile(m_pythonSourcePath + "relation_features.py");
        //interpreter.execfile(m_pythonSourcePath + "train.py");

        // match given csv input, note: you may need to adjust the exact timestamp following /model/
        interpreter.execfile(m_pythonSourcePath + "cal_column_similarity.py -p Test\\ Data/self -m /model/2023-04-05-23-58-28 -s one-to-one");
    }

    public void parseInput(String[] truth, String[] alternation) {
        assert(truth.length == 1 && alternation.length == 1): "project_evaluator: XGBoost: Expecting exactly two .csv files as input";

        // init paths
        final var truthSource = Paths.get(truth[0]);
        final var alternationSource = Paths.get(alternation[0]);
        final var truthDestination = Paths.get(m_pythonSourcePath + "/Test\\ Data/self/Table1.csv");
        final var alternationDestination = Paths.get(m_pythonSourcePath + "/Test\\ Data/self/Table2.csv");

        // copy files to Python resources folder
        try {
            Files.copy(truthSource, truthDestination);
            Files.copy(alternationSource, alternationDestination);
        } catch (IOException e) {
            throw new RuntimeException("project_evaluator: XGBoost: Failed to copy file: " + e.getMessage());
        }
    }

    public List<Correspondence<Node>> parseOutput() {
        var correspondences = new ArrayList<Correspondence<Node>>();

        try (CSVReader reader = new CSVReader(new FileReader(m_csvOutputPath))) {
            final List<String[]> data = reader.readAll();
            for (String[] row : data) {
                assert(row.length == data.size()): "project_evaluator: XGBoost: Illegal .csv output file.";

                final var truthRecord = row[0]; //row[0] == record to be matched

                // read similarity to all alternation records
                for (int i = 1; i < row.length; i++) {
                    final var alternationRecord = data.get(0)[i]; //data.get(0) == header row of csv
                    correspondences.add(new Correspondence<>(
                            new Node(truthRecord),
                            new Node(alternationRecord),
                            Double.parseDouble(row[i])
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("project_evaluator: XGBoost: Failed to read matcher output file: " + e.getMessage());
        }

        return correspondences;
    }
}
