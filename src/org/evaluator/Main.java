package org.evaluator;

import edu.stanford.nlp.util.Quadruple;
import edu.stanford.nlp.util.Triple;
import org.converter.YAMLtoTruthCorrespondences;
import org.javatuples.Quintet;
import org.utils.Correspondence;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("You need to pass at least one -alt and -o argument for the program to work.");
            System.exit(1);
        }

        var alterationFiles = new ArrayList<Quintet<String, String, String, String, String>>();
        var outfile = "";

        // arguments are expected to be: truth Correspondences (YAML), truthYAML, truthCSV, alterationYAML, alterationCSV
        for (var i = 0; i < args.length; i++) {
            if (args[i].startsWith("-alt") && args.length > i + 5) {
                alterationFiles.add(new Quintet<>(args[i + 1], args[i + 2], args[i + 3], args[i + 4], args[i + 5]));
            }
            if (args[i].startsWith("-o") && args.length > i + 1) {
                outfile = args[i + 1];
            }
        }
        assert !outfile.equals("");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outfile));
        } catch (IOException e) {
            System.err.println("Outputfile could not be opened.");
            System.exit(1);
        }
        var yaml = new Yaml();
        var yamlWriter = new StringWriter();


        AtomicInteger step = new AtomicInteger(1);
        System.out.println("Starting analysis of " + alterationFiles.size() + "schema relations.\n" +
                "This will take " + 3 * alterationFiles.size() + "steps.\n\n" +
                "Currently at step " + step + "/" + 3 * alterationFiles.size());

        for (Quintet<String, String, String, String, String> input : alterationFiles) {
            var yamlTruth = input.getValue1();
            var yamlAlteration = input.getValue3();
            var csvTruth = input.getValue2().split(",");
            var csvAlteration = input.getValue4().split(",");
            var truthCorrespondences = YAMLtoTruthCorrespondences.convert(input.getValue0());

            System.out.print("\rCurrently at step " + step.getAndIncrement() + "/" + 3 * alterationFiles.size());
            var outSF = Matcher.matchSimilarityFlooding(yamlTruth, yamlAlteration);
            var cmSF = CorrespondenceAnalyzer.Analyze(truthCorrespondences, outSF);
            //writeToFile(yaml, writer, yamlWriter, csvTruth, alteration, outSF, cmSF);

            System.out.print("\rCurrently at step " + step.getAndIncrement() + "/" + 3 * alterationFiles.size());
            var outW = Matcher.matchWinter(csvTruth, csvAlteration);
            var cmW = CorrespondenceAnalyzer.Analyze(truthCorrespondences, outW);
            //writeToFile(yaml, writer, yamlWriter, truth, alteration, outW, cmW);

            System.out.print("\rCurrently at step " + step.getAndIncrement() + "/" + 3 * alterationFiles.size());
            var outXG = Matcher.matchXG(csvTruth, csvAlteration);
            var cmXG = CorrespondenceAnalyzer.Analyze(truthCorrespondences, outXG);
            //writeToFile(yaml, writer, yamlWriter, truth, alteration, outXG, cmXG);
        }
    }

    public static void writeToFile(Yaml yaml,
                                   BufferedWriter writer,
                                   StringWriter yamlWriter,
                                   String truth,
                                   String alteration,
                                   List<? extends Correspondence<?>> correspondences,
                                   ConfusionMatrix confMatrix) {
        yaml.dump(new Quadruple<>(
                Arrays.stream(truth.split("/")).reduce((first, second) -> second).orElse(null),
                Arrays.stream(alteration.split("/")).reduce((first, second) -> second).orElse(null),
                correspondences,
                new double[]{
                        confMatrix.truePositives(),
                        confMatrix.TruePositiveRate(),
                        confMatrix.Precision()}
        ), yamlWriter);
        try {
            writer.append(yamlWriter.toString());
        } catch (IOException e) {
            System.err.println("Could not write to output file.");
            System.exit(1);
        }
    }
}