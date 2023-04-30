package org.evaluator;

import de.uni_mannheim.informatik.dws.winter.matching.MatchingEngine;
import de.uni_mannheim.informatik.dws.winter.matching.blockers.BlockingKeyIndexer.VectorCreationMethod;
import de.uni_mannheim.informatik.dws.winter.model.*;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.*;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.blocking.DefaultAttributeValuesAsBlockingKeyGenerator;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.comparators.LabelComparatorJaccard;
import de.uni_mannheim.informatik.dws.winter.processing.Processable;
import de.uni_mannheim.informatik.dws.winter.similarity.vectorspace.VectorSpaceMaximumOfContainmentSimilarity;

import org.SimilarityFlooding.Algorithms.*;
import org.SimilarityFlooding.DataTypes.*;
import org.SimilarityFlooding.FixpointFormula;
import org.SimilarityFlooding.SFConfig;
import org.SimilarityFlooding.SimilarityFlooding;
import org.SimilarityFlooding.Util.YAMLParser;

import java.io.File;
import java.io.FileNotFoundException;
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

    // DUPLIKATBASIERT
    // load data

    private static final Logger logger = WinterLogManager.activateLogger("default");

    public static List<org.utils.Correspondence<String>> matchWinterDuplicate(String[] truth, String[] alternation) {
        DataSet<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> data1 = new HashedDataSet<>();
        try {
            new CSVRecordReader(0).loadFromCSV(new File(
                    "D:\\Uni\\5\\Projekt\\roject_evaluator\\src\\org\\evaluator\\legalacts1.csv"),
                    data1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DataSet<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> data2 = new HashedDataSet<>();
        try {
            new CSVRecordReader(0).loadFromCSV(new File(
                    "D:\\Uni\\5\\Projekt\\roject_evaluator\\src\\org\\evaluator\\legalacts2.csv"),
                    data2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // load duplicates
        Processable<Correspondence<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute>> duplicates = Correspondence
                .loadFromCsv(new File(
                        "D:\\Uni\\5\\Projekt\\roject_evaluator\\src\\org\\evaluator\\legalacts_correspondences.csv"),
                        data1, data2);

        // define the schema matching rule
        VotingMatchingRule<Attribute, de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record> schemaRule = new VotingMatchingRule<Attribute, Record>(
                1.0) {

            private static final long serialVersionUID = 1L;

            public double compare(Attribute a1, Attribute a2,
                    Correspondence<Record, Matchable> c) {
                // get both attribute values
                String value1 = c.getFirstRecord().getValue(a1);
                String value2 = c.getSecondRecord().getValue(a2);

                // check if they are equal
                if (value1 != null && value2 != null && !value1.equals("0.0") && value1.equals(value2)) {
                    return 1.0;
                } else {
                    return 0.0;
                }
            }
        };

        // initialize matching engine
        MatchingEngine<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> engine = new MatchingEngine<>();

        // execute the matching
        Processable<Correspondence<Attribute, de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record>> correspondences = null;
        try {
            correspondences = engine.runDuplicateBasedSchemaMatching(data1.getSchema(), data2.getSchema(), duplicates,
                    (VotingMatchingRule<Attribute, Record>) schemaRule, null, new VotingAggregator<>(true, 1.0),
                    new NoSchemaBlocker<>());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // print results (fragen, wie genau das funtioniert)
        return correspondences.get().stream().map(c -> new org.utils.Correspondence<>(
                c.getFirstRecord().getName(),
                c.getSecondRecord().getName(),
                c.getSimilarityScore())).toList();
    }

    // INSTANZBASIERT
    public static List<org.utils.Correspondence<String>> matchWinterInstance(String[] truth, String[] alternation) {
        DataSet<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> data1 = new HashedDataSet<>();
        try {
            new CSVRecordReader(-1).loadFromCSV(new File(
                    "D:\\Uni\\5\\Projekt\\roject_evaluator\\src\\org\\evaluator\\legalacts1.csv"),
                    data1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DataSet<de.uni_mannheim.informatik.dws.winter.model.defultmodel.Record, Attribute> data2 = new HashedDataSet<>();
        try {
            new CSVRecordReader(-1).loadFromCSV(new File(
                    "D:\\Uni\\5\\Projekt\\roject_evaluator\\src\\org\\evaluator\\legalacts2.csv"),
                    data2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // define a blocker that uses the attribute values to generate pairs
        InstanceBasedSchemaBlocker<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> blocker = new InstanceBasedSchemaBlocker<>(
                new DefaultAttributeValueGenerator(data1.getSchema()),
                new DefaultAttributeValueGenerator(data2.getSchema()));

        // to calculate the similarity score, aggregate the pairs by counting
        // and normalise with the number of record in the smaller dataset
        // (= the maximum number of records that can match)
        VotingAggregator<Attribute, MatchableValue> aggregator = new VotingAggregator<>(
                false,
                Math.min(data1.size(), data2.size()),
                0.0);

        // initialize matching engine
        MatchingEngine<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> engine = new MatchingEngine<>();

        // run the matching engine
        Processable<Correspondence<Attribute, MatchableValue>> correspondences = null;
        try {
            correspondences = engine.runInstanceBasedSchemaMatching(data1, data2,
                    new DefaultAttributeValuesAsBlockingKeyGenerator(data1.getSchema()),
                    new DefaultAttributeValuesAsBlockingKeyGenerator(data2.getSchema()),
                    VectorCreationMethod.TermFrequencies,
                    new VectorSpaceMaximumOfContainmentSimilarity(), 0.0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // print results (fragen, wie genau das funtioniert)
        return correspondences.get().stream().map(c -> new org.utils.Correspondence<>(
                c.getFirstRecord().getName(),
                c.getSecondRecord().getName(),
                c.getSimilarityScore())).toList();
    }

    // LABELBASIERT
    public static List<org.utils.Correspondence<String>> matchWinterLabel(String[] truth, String[] alternation) {
        DataSet<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> data1 = new HashedDataSet<>();
        try {
            new CSVRecordReader(0).loadFromCSV(new File(
                    "D:\\Uni\\5\\Projekt\\roject_evaluator\\src\\org\\evaluator\\legalacts1.csv"),
                    data1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        DataSet<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> data2 = new HashedDataSet<>();
        try {
            new CSVRecordReader(0).loadFromCSV(new File(
                    "D:\\Uni\\5\\Projekt\\roject_evaluator\\src\\org\\evaluator\\legalacts2.csv"),
                    data1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MatchingEngine<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> engine = new MatchingEngine<>();

        Processable<Correspondence<Attribute, Attribute>> correspondences = null;
        try {
            correspondences = engine.runLabelBasedSchemaMatching(data1.getSchema(), data2.getSchema(),
                    new LabelComparatorJaccard(), 0.5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return correspondences.get().stream().map(c -> new org.utils.Correspondence<>(
                c.getFirstRecord().getName(),
                c.getSecondRecord().getName(),
                c.getSimilarityScore())).toList();
    }

    public static List<org.utils.Correspondence<String>> matchXG(String[] truth, String[] alternation) {
        return new ArrayList<>();
    }
}
