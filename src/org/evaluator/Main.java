package org.evaluator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.jena.base.Sys;
import org.converter.CorrespondanceExtractor;
import org.converter.Node;
import org.converter.YAMLParser;
import org.types.EvaluationResult;
import org.utils.Correspondence;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
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
            if (args[i].startsWith("-correspondences") && args.length > i + 1) {
                corrFiles.addAll(Arrays.stream(args[i + 1].split(",")).toList());
                i++;
            }
            if (args[i].startsWith("-av") && args.length > i + 1) {
                outDir = args[i + 1];
                i++;
            }
        }
        assert !outDir.isEmpty();

        System.out.println(schemaFiles);
        final int numberSchemata = schemaFiles.stream()
                .map(schemaFile -> schemaFile.split("/"))
                .map(path -> Integer.parseInt(path[path.length - 1].substring(0,2)))
                .max(Integer::compare).orElse(0);

        final var finalOutDir = outDir;

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

        final var firstInstances = instances.stream().filter(instance -> instance.contains(String.format("%02d", firstSchema))).toArray(String[]::new);
        final var secondInstances = instances.stream().filter(instance -> instance.contains(String.format("%02d", secondSchema))).toArray(String[]::new);
        final var firstGraph = YAMLParser.ParseGraph(schemas.get(firstSchema - 1)).orElse(null);
        final var secondGraph = YAMLParser.ParseGraph(schemas.get(secondSchema - 1)).orElse(null);
        writeToFile(
                new EvaluationResult(
                        CorrespondenceAnalyzer.Analyze(
                                correspondences,
                                Matcher.matchSimilarityFlooding(
                                        firstGraph,
                                        secondGraph)),
                        /*CorrespondenceAnalyzer.Analyze(
                                correspondences,
                                Matcher.matchWinterDuplicate(firstInstances, secondInstances)),
                        CorrespondenceAnalyzer.Analyze(
                                correspondences,
                                Matcher.matchWinterInstance(firstInstances, secondInstances)),*/
                        CorrespondenceAnalyzer.Analyze(
                                correspondences,
                                Matcher.matchWinterLabel(
                                        (String[]) firstGraph.nodes().stream().map(Node::name).toArray(),
                                        (String[]) secondGraph.nodes().stream().map(Node::name).toArray())),
                        /*CorrespondenceAnalyzer.Analyze(
                                correspondences,
                                Matcher.matchXG(firstInstances, secondInstances)),*/
                        correspondences),
                outDir + String.format("/%02d-%02d-evaluation.yaml", firstSchema, secondSchema));
    }

    public static void writeToFile(EvaluationResult result, String outFile) {
        var mapper = new ObjectMapper(new YAMLFactory());
        try {
            var bufWriter = new BufferedWriter(new FileWriter(outFile));
            mapper.writeValue(bufWriter, result);
            bufWriter.close();
        } catch (IOException e) {
            System.err.println("Could not write to output file." + outFile);
            System.exit(1);
        }
    }
}