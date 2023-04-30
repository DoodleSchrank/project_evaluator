package org.matchers;

import org.converter.Node;
import org.utils.Correspondence;

import java.util.List;

/**
 * Interface for wrapper classes that encapsulate arbitrary schema matchers, e.g. XGBoostWrapper.
 */
public interface MatcherWrapperInterface {
    /**
     * Executes the wrapped matcher. To be used after parseInput().
     */
    void execute();

    /**
     * Setup method for underlying matcher. Prepares given truth and alternation schemas.
     *
     * @param truth paths to input truth schemas
     * @param alternation paths to input alternation schemas
     */
    void parseInput(String[] truth, String[] alternation);

    /**
     * Parses matcher output to be readable by project_evaluator.
     *
     * @return List representation of output correspondences.
     */
    List<Correspondence<Node>> parseOutput();
}
