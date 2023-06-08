package org.types;
import org.utils.Correspondence;
import java.util.List;

public record AlgorithmResult(double truePositives,
                              double truePositiveRate,
                              double precision,
                              double falseNegativeRate,
                              double falseDiscoveryRate,
                              double positiveLikelihoodRation,
                              double prevalenceThreshold,
                              double threatScore,
                              double prevalence,
                              double f1score,
                              double fowklesMallowsIndex,
                              double diagnosticOddsRatio,
                              List<? extends Correspondence<?>> correspondences) {
}
