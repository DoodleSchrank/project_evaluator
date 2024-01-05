package org.evaluator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.converter.SchematcherParser;
import org.types.Node;
import org.converter.GraphParser;
import org.types.EvaluationResult;
import org.utils.Correspondence;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        var linkedSF = false;

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

            if (args[i].startsWith("-linkedSF") && args.length > i + 1) {
                linkedSF = "true".equals(args[i + 1]);
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

        final var baseindex = schemaFiles.get(0).contains("single") ? 0 : 1;
        for(var i = baseindex; i < numberSchemata; i++) {
            for(var j = i + 1; j <= numberSchemata; j++) {

                int finalJ = j;
                int finalI = i;
                evaluate(schemaFiles.stream().filter(schema ->
                                        schema.contains(String.format("%02d", finalI))).findFirst().orElse(""),
                        schemaFiles.stream().filter(schema ->
                                        schema.contains(String.format("%02d", finalJ))).findFirst().orElse(""),
                                /*instanceFiles.stream().filter(instance ->
                                        instance.contains(String.format("%02d", finalI)) ||
                                                instance.contains(String.format("%02d", finalJ))).toList(),*/
                                GraphParser.ParseCorrespondences(corrFiles.stream().filter(corr ->
                                        corr.contains(String.format("%02d-%02d", finalI, finalJ))).findFirst().orElseThrow()),
                                finalOutDir + String.format("/%02d-%02d-evaluation.yaml", finalI, finalJ),
                        linkedSF);
            }
        }
    }

    public static void evaluate(String schemaA, String schemaB, /*List<String> instances,*/ List<Correspondence<Node>> groundTruth, String outFile, Boolean linkedSF) {
        final var schematchDBA = SchematcherParser.Parse(schemaA).orElseThrow();
        final var schematchDBB = SchematcherParser.Parse(schemaB).orElseThrow();
        System.out.println(String.join(",", schematchDBA.getTables().keySet()));
        writeToFile(
                new EvaluationResult(List.of(
                        CorrespondenceAnalyzer.Analyze(
                                "Similarity Flooding",
                                groundTruth,
                                Matcher.MatchSimilarityFlooding(
                                        GraphParser.Parse(schemaA, linkedSF).orElse(null),
                                        GraphParser.Parse(schemaB, linkedSF).orElse(null))),
                        CorrespondenceAnalyzer.Analyze(
                                "SchematchCosine",
                                groundTruth,
                                Matcher.MatchSchematchCosine(schematchDBA, schematchDBB)),
                        CorrespondenceAnalyzer.Analyze(
                                "SchematchHammingMatcher",
                                groundTruth,
                                Matcher.MatchSchematchHamming(schematchDBA, schematchDBB)),
                        CorrespondenceAnalyzer.Analyze(
                                "SchematchJaroWinkler",
                                groundTruth,
                                Matcher.MatchSchematchJaroWinkler(schematchDBA, schematchDBB)),
                        CorrespondenceAnalyzer.Analyze(
                                "SchematchLevenshtein",
                                groundTruth,
                                Matcher.MatchSchematchLevensthein(schematchDBA, schematchDBB)),
                        CorrespondenceAnalyzer.Analyze(
                                "SchematchLongestCommonSubsequenceMatcher",
                                groundTruth,
                                Matcher.MatchSchematchLongestCommonSubSequence(schematchDBA, schematchDBB))
                ),groundTruth),
                outFile);
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