package org.evaluator;

import de.uni_mannheim.informatik.dws.winter.matching.MatchingEngine;
import de.uni_mannheim.informatik.dws.winter.model.*;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.*;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.comparators.LabelComparatorJaccard;
import de.uni_mannheim.informatik.dws.winter.processing.Processable;

import org.SimilarityFlooding.Algorithms.*;
import org.SimilarityFlooding.DataTypes.*;
import org.SimilarityFlooding.FixpointFormula;
import org.SimilarityFlooding.SFConfig;
import org.SimilarityFlooding.SimilarityFlooding;
import org.SimilarityFlooding.Util.YAMLParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Matcher {
    public static List<org.utils.Correspondence<String>> matchSimilarityFlooding(String truth, String alteration) {
        Graph<String> g1 = YAMLParser.Parse(truth).orElse(null);
        Graph<String> g2 = YAMLParser.Parse(alteration).orElse(null);

        var sfconfig = new SFConfig(StringSimilarity::Levenshtein, FixpointFormula.C);
        assert g1 != null && g2 != null;
        var sf = new SimilarityFlooding<>(g1, g2, sfconfig);
        sf.run(10, 0.05f);
        var distances = sf.getCorrespondants();
        var graphs = sf.getGraphs();

        var knowledge = Filter.Knowledge;
        var ownKnowledge = new HashMap<>(knowledge);
        ownKnowledge.put("first", "erster");
        distances = Filter.knowledgeFilter(distances, ownKnowledge);
        distances = Filter.typingConstraintFilter(graphs, distances);
        distances = Filter.cardinalityConstraintFilter(distances);
        distances = Selector.highestCumulativeSimilaritySelection(graphs, distances);

        return distances;
    }

    public static List<org.utils.Correspondence<String>> matchWinter(String[] truth, String[] alternation) {
        DataSet<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> data1 = new HashedDataSet<>();
        DataSet<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> data2 = new HashedDataSet<>();
        Arrays.stream(truth).forEach(file -> {
            try {
                new CSVRecordReader(0).loadFromCSV(new File(file), data1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Arrays.stream(alternation).forEach(file -> {
            try {
                new CSVRecordReader(0).loadFromCSV(new File(file), data1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        MatchingEngine<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> engine = new MatchingEngine<>();

        Processable<Correspondence<Attribute, Attribute>> correspondences;
        try {
            correspondences = engine.runLabelBasedSchemaMatching(data1.getSchema(), data2.getSchema(), new LabelComparatorJaccard(), 0.5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return correspondences.get().stream().map(c ->
                new org.utils.Correspondence<>(
                        c.getFirstRecord().getName(),
                        c.getSecondRecord().getName(),
                        c.getSimilarityScore())).toList();
    }

    public static List<org.utils.Correspondence<String>> matchXG(String[] truth, String[] alternation) {
        return new ArrayList<>();
    }
}
