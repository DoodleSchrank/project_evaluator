package org.types;

public record ConfusionMatrix(long truePositives,
                              long falsePositives,
                              long trueNegatives,
                              long falseNegatives,
                              long positives,
                              long negatives) {
    public double TruePositiveRate() {
        return (double) truePositives / positives;
    }

    public double Sensitivity() {
        return TruePositiveRate();
    }

    public double Recall() {
        return TruePositiveRate();
    }

    public double TrueNegativeRate() {
        return (double) trueNegatives / negatives;
    }

    public double Specificity() {
        return TrueNegativeRate();
    }

    public double Selectivity() {
        return TrueNegativeRate();
    }

    public double Precision() {
        return (double) truePositives / (truePositives + falsePositives);
    }

    public double NegativePredictiveValue() {
        return (double) trueNegatives / (trueNegatives + falseNegatives);
    }

    public double FalseNegativeRate() {
        return 1.0d - TruePositiveRate();
    }

    public double MissRate() {
        return FalseNegativeRate();
    }

    public double FalsePositiveRate() {
        return 1.0d - TrueNegativeRate();
    }

    public double FallOut() {
        return FalsePositiveRate();
    }

    public double FalseDiscoveryRate() {
        return 1.0d - Precision();
    }

    public double FalseOmissionRate() {
        return 1.0d - NegativePredictiveValue();
    }

    public double PositiveLikelihoodRatio() {
        return TruePositiveRate() / FalsePositiveRate();
    }

    public double NegativeLikelihoodRatio() {
        return FalseNegativeRate() / TrueNegativeRate();
    }

    public double PrevalenceThreshold() {
        return Math.sqrt(FalsePositiveRate()) / (Math.sqrt(TruePositiveRate()) + Math.sqrt(FalsePositiveRate()));
    }

    public double ThreatScore() {
        return (double) truePositives / (truePositives + falseNegatives + falsePositives);
    }

    public double CriticalSuccessIndex() {
        return ThreatScore();
    }

    public double Prevalence() {
        return (double) positives / (positives + negatives);
    }

    public double Accuracy() {
        return (double) (truePositives + trueNegatives) / (positives + negatives);
    }

    public double BalancedAccuracy() {
        return (TruePositiveRate() + TrueNegativeRate()) / 2.0d;
    }

    public double F1Score() {
        return 2.0d * truePositives / (2.0d * truePositives + falsePositives + falseNegatives);
    }

    public double PhiCoefficient() {
        return (truePositives * trueNegatives - falsePositives * falseNegatives) /
                Math.sqrt((truePositives + falsePositives) *
                        (truePositives + falseNegatives) *
                        (trueNegatives + falsePositives) *
                        (trueNegatives + falseNegatives));
    }

    public double MatthewsCorrelationCoefficient() {
        return PhiCoefficient();
    }

    public double FowlkesMallowsIndex() {
        return Math.sqrt(Precision() * TruePositiveRate());
    }

    public double Informedness() {
        return TruePositiveRate() + TrueNegativeRate() - 1;
    }

    public double Markedness() {
        return Precision() + NegativePredictiveValue() - 1;
    }

    public double DeltaP() {
        return Markedness();
    }

    public double DiagnosticOddsRatio() {
        return PositiveLikelihoodRatio() / NegativeLikelihoodRatio();
    }

    public double[] Matrix() {
        return new double[] {
                TruePositiveRate(),
                TrueNegativeRate(),
                Precision(),
                NegativePredictiveValue(),
                MissRate(),
                FallOut(),
                FalseDiscoveryRate(),
                FalseOmissionRate(),
                PositiveLikelihoodRatio(),
                NegativeLikelihoodRatio(),
                PrevalenceThreshold(),
                ThreatScore(),
                Prevalence(),
                Accuracy(),
                BalancedAccuracy(),
                F1Score(),
                PhiCoefficient(),
                FowlkesMallowsIndex(),
                Informedness(),
                Markedness(),
                DiagnosticOddsRatio()
        };
    }
}
