package org.evaluator;

import org.SimilarityFlooding.Util.YAMLParser;
import org.converter.CorrespondanceExtractor;
import org.javatuples.Pair;
import org.types.EvaluationResult;
import org.utils.Correspondence;
import org.yaml.snakeyaml.Yaml;
import scala.concurrent.impl.FutureConvertersImpl;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("You need to pass at least one -alt and -o argument for the program to work.");
            System.exit(1);
        }

        var schemaFiles = new ArrayList<String>();
        var instanceFiles = new ArrayList<String>();
        var corrFiles = new ArrayList<String>();
        var outDir = "";

        for (var i = 0; i < args.length; i++) {
            if (args[i].startsWith("-schemata") && args.length > i + 1) {
                schemaFiles.addAll(Arrays.stream(args[i + 1].split(",")).toList());
                i++;
            }
            if (args[i].startsWith("-instances") && args.length > i + 1) {
                instanceFiles.addAll(Arrays.stream(args[i + 1].split(",")).toList());
                i++;
            }
            if (args[i].startsWith("-corrs") && args.length > i + 1) {
                corrFiles.addAll(Arrays.stream(args[i + 1].split(",")).toList());
                i++;
            }
            if (args[i].startsWith("-av") && args.length > i + 1) {
                outDir = args[i + 1];
                i++;
            }
        }
        assert !outDir.isEmpty();

        final int numberSchemata = schemaFiles.stream().map(schemaFile -> Integer.parseInt(schemaFile.substring(0, 2))).max(Integer::compare).orElse(0);

        final var finalOutDir = outDir;
        IntStream.range(1, numberSchemata).parallel().forEach(i -> {
            IntStream.range(i, numberSchemata).parallel().forEach(j -> {
                evaluate(i, j, schemaFiles.stream().filter(schema ->
                                schema.startsWith(String.format("%02d", i)) ||
                                        schema.startsWith(String.format("%02d", j))).toList(),
                        instanceFiles.stream().filter(instance ->
                                instance.startsWith(String.format("%02d", i)) ||
                                        instance.startsWith(String.format("%02d", j))).toList(),
                        CorrespondanceExtractor.ParseCorrs(corrFiles.stream().filter(corr ->
                                corr.startsWith(String.format("%02d-%02d", i, j))).findFirst().orElseThrow()).orElseThrow(), //TODO maybe multiple corr files?
                        finalOutDir);
            });
        });
    }

    public static void evaluate(int firstSchema, int secondSchema, List<String> schemas, List<String> instances, List<Correspondence<String>> correspondences, String outDir) {
        assert schemas.size() == 2;

        final var firstInstances = instances.stream().filter(instance -> instance.startsWith("%02d", firstSchema)).toArray(String[]::new);
        final var secondInstances = instances.stream().filter(instance -> instance.startsWith("%02d", secondSchema)).toArray(String[]::new);

        writeToFile(
                new EvaluationResult(
                        CorrespondenceAnalyzer.Analyze(
                                correspondences,
                                Matcher.matchSimilarityFlooding(
                                        YAMLParser.Parse(schemas.get(0)).orElse(null),
                                        YAMLParser.Parse(schemas.get(1)).orElse(null))),
                        CorrespondenceAnalyzer.Analyze(
                                correspondences,
                                Matcher.matchWinterDuplicate(firstInstances, secondInstances)),
                        CorrespondenceAnalyzer.Analyze(
                                correspondences,
                                Matcher.matchWinterInstance(firstInstances, secondInstances)),
                        CorrespondenceAnalyzer.Analyze(
                                correspondences,
                                Matcher.matchWinterLabel(firstInstances, secondInstances)),
                        CorrespondenceAnalyzer.Analyze(
                                correspondences,
                                Matcher.matchXG(firstInstances, secondInstances)),
                        correspondences),
                outDir + String.format("/%02d-%02d-evaluation.yaml", firstSchema, secondSchema));
    }

    public static void writeToFile(EvaluationResult result, String outFile) {

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outFile));
        } catch (IOException e) {
            System.err.println("Outputfile could not be opened.");
            System.exit(1);
        }
        var yaml = new Yaml();
        var yamlWriter = new StringWriter();

        yaml.dump(result, yamlWriter);
        try {
            writer.append(yamlWriter.toString());
        } catch (IOException e) {
            System.err.println("Could not write to output file.");
            System.exit(1);
        }
    }
}