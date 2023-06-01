package org.matchers;

import au.com.bytecode.opencsv.CSVReader;
import org.converter.Node;
import org.utils.Correspondence;
import org.python.util.PythonInterpreter;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

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
        //todo: fix python interpreter
        //final var interpreter = new PythonInterpreter();

        // train xgboost model (only required once)
        //interpreter.execfile(m_pythonSourcePath + "relation_features.py");
        //interpreter.execfile(m_pythonSourcePath + "train.py");


        // match given csv input, note: you may need to adjust the exact timestamp following /model/
        //interpreter.execfile(m_pythonSourcePath + "/cal_column_similarity.py -p Test\\ Data/self -m /model/2023-04-05-23-58-28 -s one-to-one");

        //temporary hotfix: manually execute the python script(s) in resources and then proceed execution (todo)
        final var scanner = new Scanner(System.in);
        System.out.println("Please train the XGBoost model and then execute in 'resources/Python-Schema-Matching': ");
        System.out.println("python3 cal_column_similarity.py -p Test\\ Data/self -m /model/2023-04-05-23-58-28 -s one-to-one");
        System.out.print("Press enter to continue execution...");
        String input = scanner.nextLine(); // Wait for user input
    }

    public void parseInput(String[] truth, String[] alternation) {
        assert(truth.length == 1 && alternation.length == 1): "project_evaluator: XGBoost: Expecting exactly two .csv files as input";

        // init paths
        final var truthSource = Paths.get(System.getProperty("user.dir") + "/" + truth[0]);
        final var alternationSource = Paths.get(System.getProperty("user.dir") + "/" + alternation[0]);
        final var truthDestination = Paths.get(m_pythonSourcePath + "/Test Data/self/Table1.csv");
        final var alternationDestination = Paths.get(m_pythonSourcePath + "/Test Data/self/Table2.csv");

        // copy files to Python resources folder
        try {
            //note: you need to remove Table1 and Table2 manually. todo: automate
            Files.copy(truthSource, truthDestination, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(alternationSource, alternationDestination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("project_evaluator: XGBoost: Failed to copy file: " + e.getMessage());
        }
    }


    //todo: what about the label .csv file?
        public List<Correspondence<Node>> parseOutput() {
        var correspondences = new ArrayList<Correspondence<Node>>();

        try (CSVReader reader = new CSVReader(new FileReader(m_csvOutputPath + "/similarity_matrix_value.csv"))) {
            final List<String[]> data = reader.readAll();

            //drop first csv row containing col name
            for (String[] row_raw : data.subList(1, data.size())) {
                assert(row_raw.length == data.size()): "project_evaluator: XGBoost: Illegal .csv output file.";

                //drop first row col containing col name
                final var row_processed = Arrays.copyOfRange(row_raw,1, row_raw.length);

                final var truthRecord = row_processed[0]; //row[0] == record to be matched

                // read similarity to all alternation records
                for (int i = 1; i < row_processed.length; i++) {
                    final var alternationRecord = data.get(0)[i]; //data.get(0) == header row of csv
                    correspondences.add(new Correspondence<>(
                            new Node(truthRecord),
                            new Node(alternationRecord),
                            Double.parseDouble(row_processed[i])
                    ));
                }
            }
        } catch (Exception e) {
            System.out.println("Expected Python .csv output location: " + m_csvOutputPath + "/similarity_matrix_value.csv");
            throw new RuntimeException("project_evaluator: XGBoost: Failed to read matcher output file. Please double check the Python output exists. " + e.getMessage());
        }

        return correspondences;
    }
}
