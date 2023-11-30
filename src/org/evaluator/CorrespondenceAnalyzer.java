package org.evaluator;

import org.converter.Node;
import org.types.AlgorithmResult;
import org.types.ConfusionMatrix;
import org.utils.Correspondence;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import static java.util.stream.Collectors.toCollection;

public class CorrespondenceAnalyzer {
    public static AlgorithmResult Analyze(List<? extends Correspondence<?>> truth, List<? extends Correspondence<?>> unsortedAlgResult) {
        final var algorithmResult = unsortedAlgResult.stream().collect(toCollection(ArrayList::new));
        algorithmResult.sort(Comparator.comparingDouble(Correspondence::similarity));
        Collections.reverse(algorithmResult);

        if (algorithmResult.isEmpty()) {
            var cm =  new ConfusionMatrix(
                    0,
                    0,
                    0,
                    0,
                    truth.size(),
                    0,
                    0
            );
            return new AlgorithmResult(
                    cm.truePositives(),
                    cm.TruePositiveRate(),
                    cm.Precision(),
                    cm.FalseNegativeRate(),
                    cm.FalseDiscoveryRate(),
                    cm.PositiveLikelihoodRatio(),
                    cm.PrevalenceThreshold(),
                    cm.ThreatScore(),
                    cm.Prevalence(),
                    cm.F1Score(),
                    cm.FowlkesMallowsIndex(),
                    cm.DiagnosticOddsRatio(),
                    cm.recallGT(),
                    algorithmResult);
        }

        final var usesIds = algorithmResult.get(0).nodeA() instanceof Node && !((Node) algorithmResult.get(0).nodeA()).id().toString().equals("-1");

        // create crossproduct of all possible correspondences
        final var crossProduct = new HashSet<Correspondence<?>>(truth);
        Collections.synchronizedList(truth).parallelStream()
                .map(Correspondence::nodeA)
                .forEach(nodeA -> {
                    truth.stream()
                            .map(Correspondence::nodeB)
                            .forEach(nodeB ->
                                    crossProduct.add(new Correspondence<>(nodeA, nodeB, 0.0d)));
                });

        long truePositive;
        double recallGT;

        if (usesIds) {
            truePositive = truth.stream().filter(algorithmResult::contains).count();
            recallGT = ((double) algorithmResult.stream().limit(truth.size())
                    .filter(truth::contains).count()) / ((double) truth.size());
        } else {
            truePositive = algorithmResult.parallelStream()
                    .filter(ar -> truth.stream().anyMatch(t -> t.toString().equals(ar.toString()))).count();
            recallGT = ((double) algorithmResult.stream().limit(truth.size())
                    .filter(truth::contains).count()) / ((double) truth.size());
        }
        final var falsePositive = algorithmResult.size() - truePositive;

        final var trueNegative = crossProduct.parallelStream()
                .filter(cP -> !truth.contains(cP))    // get all real true negatives
                .filter(tN -> !algorithmResult.contains(tN))    // extract the ones correct in the testresults
                .count();
        final var falseNegative = crossProduct.parallelStream()
                .filter(cP -> !algorithmResult.contains(cP))    // get test negatives
                .filter(truth::contains)              // which are supposed to be positive
                .count();

        var cm =  new ConfusionMatrix(
                truePositive,
                falsePositive,
                trueNegative,
                falseNegative,
                truth.size(),
                crossProduct.parallelStream().filter(cP -> !truth.contains(cP)).count(),
                recallGT
        );
        return new AlgorithmResult(
                cm.truePositives(),
                cm.TruePositiveRate(),
                cm.Precision(),
                cm.FalseNegativeRate(),
                cm.FalseDiscoveryRate(),
                cm.PositiveLikelihoodRatio(),
                cm.PrevalenceThreshold(),
                cm.ThreatScore(),
                cm.Prevalence(),
                cm.F1Score(),
                cm.FowlkesMallowsIndex(),
                cm.DiagnosticOddsRatio(),
                cm.recallGT(),
                algorithmResult);
    }
}
