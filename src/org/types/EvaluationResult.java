package org.types;

import org.utils.Correspondence;

import java.util.List;

public record EvaluationResult(List<AlgorithmResult> Results,
                               List<? extends  Correspondence<?>> TrueCorrespondences) {
}
