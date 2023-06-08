package org.evaluator;

import org.types.AlgorithmResult;
import org.types.ConfusionMatrix;
import org.utils.Correspondence;

import java.util.ArrayList;
import java.util.List;

public class CorrespondenceAnalyzer {
    public static AlgorithmResult Analyze(List<? extends Correspondence<?>> truth, List<? extends Correspondence<?>> algorithmResult) {

        // create crossproduct of all possible correspondences
        var crossProduct = new ArrayList<>();
        truth.parallelStream()
                .map(Correspondence::nodeA).distinct()
                .forEach(uniqueFirst -> {
                    truth.stream()
                            .map(Correspondence::nodeB).distinct()
                            .forEach(uniqueSecond ->
                                    crossProduct.add(new Correspondence<>(uniqueFirst, uniqueSecond, 0.0d)));
                });

        var truePositive = algorithmResult.parallelStream()
                .filter(truth::contains).count();
        var falsePositive = algorithmResult.size() - truePositive;

        var trueNegative = crossProduct.parallelStream()
                .filter(cP -> !truth.contains(cP))    // get all real true negatives
                .filter(tN -> !algorithmResult.contains(tN))    // extract the ones correct in the testresults
                .count();
        var falseNegative = crossProduct.parallelStream()
                .filter(cP -> !algorithmResult.contains(cP))    // get test negatives
                .filter(truth::contains)              // which are supposed to be positive
                .count();

        var cm =  new ConfusionMatrix(
                truePositive,
                falsePositive,
                trueNegative,
                falseNegative,
                truth.size(),
                crossProduct.parallelStream().filter(cP -> !truth.contains(cP)).count()
        );
        return new AlgorithmResult(cm.truePositives(),
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
                algorithmResult);
    }
}
