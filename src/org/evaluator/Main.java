package org.evaluator;

import org.apache.jena.base.Sys;
import org.converter.CorrespondanceExtractor;
import org.converter.Node;
import org.converter.YAMLParser;
import org.types.EvaluationResult;
import org.utils.Correspondence;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

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

        final int numberSchemata = schemaFiles.stream().map(schemaFile -> Integer.parseInt(schemaFile.split("(?=[0-9][0-9])")[1].substring(0, 2))).max(Integer::compare).orElse(0);

        final var finalOutDir = outDir;
        /*IntStream.range(1, numberSchemata)//.parallel()
                .forEach(i ->
                IntStream.range(i + 1, numberSchemata)//.parallel()
                        .forEach(j ->*/
        for(var i = 1; i < numberSchemata; i++) {
            for(var j = i + 1; j <= numberSchemata; j++) {

                int finalJ = j;
                int finalI = i;
                evaluate(i, j, schemaFiles.stream().filter(schema ->
                                        schema.contains(String.format("%02d", finalI)) ||
                                                schema.contains(String.format("%02d", finalJ))).toList(),
                                instanceFiles.stream().filter(instance ->
                                        instance.contains(String.format("%02d", finalI)) ||
                                                instance.contains(String.format("%02d", finalJ))).toList(),
                                YAMLParser.ParseCorrespondences(corrFiles.stream().filter(corr ->
                                        corr.contains(String.format("%02d-%02d", finalI, finalJ))).findFirst().orElseThrow()),
                                finalOutDir);
            }
        }
    }

    public static void evaluate(int firstSchema, int secondSchema, List<String> schemas, List<String> instances, List<Correspondence<Node>> correspondences, String outDir) {
        assert schemas.size() == 2;

        final var firstInstances = instances.stream().filter(instance -> instance.startsWith("%02d", firstSchema)).toArray(String[]::new);
        final var secondInstances = instances.stream().filter(instance -> instance.startsWith("%02d", secondSchema)).toArray(String[]::new);

        final var smResult = CorrespondenceAnalyzer.Analyze(
                correspondences,
                Matcher.matchSimilarityFlooding(
                        YAMLParser.ParseGraph(schemas.get(0)).orElse(null),
                        YAMLParser.ParseGraph(schemas.get(1)).orElse(null)));

        System.out.println(smResult);

        /*writeToFile(
                new EvaluationResult(
                        CorrespondenceAnalyzer.Analyze(
                                correspondences,
                                Matcher.matchSimilarityFlooding(
                                        YAMLParser.ParseGraph(schemas.get(0)).orElse(null),
                                        YAMLParser.ParseGraph(schemas.get(1)).orElse(null))),
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
                outDir + String.format("/%02d-%02d-evaluation.yaml", firstSchema, secondSchema));*/
    }

    public static void writeToFile(EvaluationResult result, String outFile) {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outFile));
        } catch (IOException e) {
            System.err.println("Outputfile could not be opened.");
            System.exit(1);
        }
        var yaml = new Yaml(options);
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