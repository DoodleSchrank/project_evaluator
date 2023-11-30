package org.types;

import org.utils.Correspondence;

import java.util.List;

public record AlgorithmResult(long truePositives,
                              double truePositiveRate,
                              double precision,
                              double falseNegativeRate,
                              double falseDiscoveryRate,
                              double positiveLikelihoodRatio,
                              double prevalenceThreshold,
                              double threatScore,
                              double prevalence,
                              double f1score,
                              double fowklesMallowsIndex,
                              double diagnosticOddsRatio,
                              double recallGT,
                              List<? extends Correspondence<?>> correspondences) {
    public String toString() {
        return String.format("True Positives: %d\nTrue Positive Rate: %f\nPrecision: %f\nFalse Negative Rate: %f\nFalse Discovery Rate: %f\nPositive Likelihood Ratio: %f\nPrevalence Threshold: %f\nThreat Score: %f\nPrevalence: %f\nF1-Score: %f\nFowkles Mallows Index: %f\nDiagnostic Odds Ratio: %f\nRecall@Ground Truth: %f\n",
                truePositives, truePositiveRate, precision, falseNegativeRate, falseDiscoveryRate, positiveLikelihoodRatio, prevalenceThreshold, threatScore, prevalence, f1score, fowklesMallowsIndex, diagnosticOddsRatio, recallGT);
    }
}
