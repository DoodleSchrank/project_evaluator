package org.types;

import org.utils.Correspondence;

import java.util.List;

public record EvaluationResult(AlgorithmResult SFResult,
                               //AlgorithmResult WinterDuplicateResult,
                               //AlgorithmResult WinterInstanceResult,
                               AlgorithmResult WinterLabelResult,
                               //AlgorithmResult XGBoostResult,
                               List<? extends  Correspondence<?>> TrueCorrespondences) {
}
