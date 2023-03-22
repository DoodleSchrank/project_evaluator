package org.evaluator;

import org.utils.Correspondence;

import java.util.ArrayList;
import java.util.List;

public class CorrespondenceAnalyzer {
    public static ConfusionMatrix Analyze(List<Correspondence<?>> testResults, List<Correspondence<?>> groundTruth) {

        // create crossproduct of all possible correspondences
        var crossProduct = new ArrayList<>();
        groundTruth.parallelStream()
                .map(Correspondence::nodeA).distinct()
                .forEach(uniqueFirst -> {
                    groundTruth.stream()
                            .map(Correspondence::nodeB).distinct()
                            .forEach(uniqueSecond ->
                                    crossProduct.add(new Correspondence<>(uniqueFirst, uniqueSecond, 0.0d)));
                });

        var truePositive = testResults.parallelStream()
                .filter(groundTruth::contains).count();
        var falsePositive = testResults.size() - truePositive;

        var trueNegative = crossProduct.parallelStream()
                .filter(cP -> !groundTruth.contains(cP))    // get all real true negatives
                .filter(tN -> !testResults.contains(tN))    // extract the ones correct in the testresults
                .count();
        var falseNegative = crossProduct.parallelStream()
                .filter(cP -> !testResults.contains(cP))    // get test negatives
                .filter(groundTruth::contains)              // which are supposed to be positive
                .count();

        return new ConfusionMatrix(
                truePositive,
                falsePositive,
                trueNegative,
                falseNegative,
                groundTruth.size(),
                crossProduct.parallelStream().filter(cP -> !groundTruth.contains(cP)).count()
        );
    }
}
