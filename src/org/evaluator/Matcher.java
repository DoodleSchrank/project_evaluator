package org.evaluator;


import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.matching.similarity.label.*;
import de.uni_marburg.schematch.matchtask.tablepair.TablePair;
import org.SimilarityFlooding.Algorithms.*;
import org.SimilarityFlooding.DataTypes.*;
import org.SimilarityFlooding.FixpointFormula;
import org.SimilarityFlooding.SFConfig;
import org.SimilarityFlooding.SimilarityFlooding;
import org.types.Node;
import org.utils.Correspondence;
import org.w3c.rdf.model.Model;
import org.w3c.rdf.model.ModelException;
import org.w3c.rdf.model.Resource;
import org.w3c.rdf.util.RDFFactoryImpl;

import java.util.*;

public class Matcher {

    private static Resource getResource(String name) {
        final var rf = new RDFFactoryImpl();
        final var nf = rf.getNodeFactory();
        try {
            return nf.createResource(name);
        } catch (ModelException e) {
            throw new RuntimeException(e);
        }
    }

    private static Model createModel(Graph<?> g1) {
        final var rf = new RDFFactoryImpl();
        final var nf = rf.getNodeFactory();

        // create graph/model A
        final var model = rf.createModel();
        g1.edges().forEach(edge -> {
            try {
                model.add(nf.createStatement(
                        getResource(edge.parent().toString()),
                        getResource(edge.relation()),
                        getResource(edge.child().toString())
                ));
            } catch (ModelException e) {
                throw new RuntimeException(e);
            }
        });
        return model;
    }

    public static List<org.utils.Correspondence<Node>> MatchSimilarityFlooding(Graph<Node> g1, Graph<Node> g2) {
        assert g1 != null && g2 != null;

        final var sfconfig = new SFConfig(StringSimilarity::Levenshtein, FixpointFormula.Basic);
        final var sf = new SimilarityFlooding<>(g1, g2, sfconfig);
        sf.run(100, 0.05f);
        var distances = sf.getCorrespondants();
        final var graphs = sf.getGraphs();

        /*final var modelA = createModel(g1);
        final var modelB = createModel(g2);
        final var initialSigma = new ArrayList<MapPair>();
        try {
            RDFUtil.getNodes(modelA).values().forEach(nA -> {
                try {
                    RDFUtil.getNodes(modelB).values().forEach(nB -> {
                        initialSigma.add(new MapPair(nA, nB, StringSimilarity.Levenshtein(nA.toString(), nB.toString())));
                    });
                } catch (ModelException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (ModelException e) {
            throw new RuntimeException(e);
        }

        final var sf_ref = new Match();
        sf_ref.formula = new boolean[]{false, false, true};
        sf_ref.formula = new boolean[]{true, true, true};
        sf_ref.FLOW_GRAPH_TYPE = 1;
        MapPair[] result;
        try {
            result = sf_ref.getMatch(modelA, modelB, initialSigma);
        } catch (ModelException e) {
            throw new RuntimeException(e);
        }
        MapPair.sort(result);
        final var filteredBest = new FilterBest().getFilterBest(Arrays.asList(result), true);*/


        distances = Filter.typingConstraintFilter(graphs, distances);
        //distances = Selector.selectThreshold(graphs, distances, 0.6f);
        distances = Selector.selectSimpleThreshold(distances, 0.70f);
        return distances;
    }

    /*
    public static @NotNull List<org.utils.Correspondence<String>> matchWinterDuplicate(String[] firstSchema, String[] secondSchema) {
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
        Processable<Correspondence<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute>> duplicates = null;
        try {
            duplicates = Correspondence
                    .loadFromCsv(new File(
                                    "D:\\Uni\\5\\Projekt\\roject_evaluator\\src\\org\\evaluator\\legalacts_correspondences.csv"),
                            data1, data2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // define the schema matching rule
        VotingMatchingRule<Attribute, Record> schemaRule = new VotingMatchingRule<>(
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
        DataSet<Record, Attribute> data1 = new HashedDataSet<>();
        DataSet<Record, Attribute> data2 = new HashedDataSet<>();
        try {
            for (var file : firstSchema) {
                new CSVRecordReader(0).loadFromCSV(new File(file), data1);
            }
            for (var file : secondSchema) {
                new CSVRecordReader(0).loadFromCSV(new File(file), data2);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
// create a matching rule
        LinearCombinationMatchingRule<Record, Attribute> matchingRule = new LinearCombinationMatchingRule<>(0.7);
// add comparators
        matchingRule.addComparator(
                (m1,  m2, c) -> new TokenizingJaccardSimilarity().calculate(m1.getValue() getTitle(), m2.getTitle()), 0.8);
        matchingRule.addComparator(
                (m1, m2, c) -> new YearSimilarity(10).calculate(m1.getDate(), m2.getDate()), 0.2);


        return correspondences.get().stream().map(c -> new org.utils.Correspondence<>(
                c.getFirstRecord().getName(),
                c.getSecondRecord().getName(),
                c.getSimilarityScore())).toList();
        return new ArrayList<>();
    }

    // INSTANZBASIERT
    public static List<org.utils.Correspondence<String>> matchWinterInstance(String[] firstSchema, String[] secondSchema) {
        DataSet<Record, Attribute> data1 = new HashedDataSet<>();
        DataSet<Record, Attribute> data2 = new HashedDataSet<>();
        try {
            for (var file : firstSchema) {
                new CSVRecordReader(-1).loadFromCSV(new File(file), data1);
            }
            for (var file : secondSchema) {
                new CSVRecordReader(-1).loadFromCSV(new File(file), data2);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // define a blocker that uses the attribute values to generate pairs
        InstanceBasedSchemaBlocker<Record, Attribute> blocker = new InstanceBasedSchemaBlocker<>(
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
        Processable<Correspondence<Attribute, MatchableValue>> correspondences;
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
    public static List<org.utils.Correspondence<String>> matchWinterLabel(List<String> firstSchema, List<String> secondSchema) {
        DataSet<Record, Attribute> data1 = new HashedDataSet<>();
        DataSet<Record, Attribute> data2 = new HashedDataSet<>();
        try {
            for (var file : firstSchema) {
                new CSVRecordReader(0).loadFromCSV(new File(file), data1);
            }
            for (var file : secondSchema) {
                new CSVRecordReader(0).loadFromCSV(new File(file), data2);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MatchingEngine<de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Record, Attribute> engine = new MatchingEngine<>();

        Processable<Correspondence<Attribute, Attribute>> correspondences;
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
    }*/

    public static List<org.utils.Correspondence<String>> MatchSchematchCosine(Database schemaA, Database schemaB) {
        return evalSchematch(new CosineMatcher(), schemaA, schemaB).stream().filter(c -> c.similarity() > 0.7f).toList();
    }
    public static List<org.utils.Correspondence<String>> MatchSchematchHamming(Database schemaA, Database schemaB) {
        return evalSchematch(new HammingMatcher(), schemaA, schemaB).stream().filter(c -> c.similarity() > 0.7f).toList();
    }
    public static List<org.utils.Correspondence<String>> MatchSchematchJaroWinkler(Database schemaA, Database schemaB) {
        return evalSchematch(new JaroWinklerMatcher(), schemaA, schemaB).stream().filter(c -> c.similarity() > 0.7f).toList();
    }
    public static List<org.utils.Correspondence<String>> MatchSchematchLevensthein(Database schemaA, Database schemaB) {
        return evalSchematch(new LevenshteinMatcher(), schemaA, schemaB).stream().filter(c -> c.similarity() > 0.7f).toList();
    }
    public static List<org.utils.Correspondence<String>> MatchSchematchLongestCommonSubSequence(Database schemaA, Database schemaB) {
        return evalSchematch(new LongestCommonSubsequenceMatcher(), schemaA, schemaB).stream().filter(c -> c.similarity() > 0.7f).toList();
    }
    private static List<Correspondence<String>> evalSchematch(LabelSimilarityMatcher matcher, Database schemaA, Database schemaB) {
        final var correspondences = new ArrayList<org.utils.Correspondence<String>>();

        for(final var tblA : schemaA.getTables().values()) {
            for (final var tblB : schemaB.getTables().values()) {
                final var result = matcher.match(new TablePair(tblA, tblB));
                for(var y = 0; y < result.length; y++) {
                    for (var x = 0; x < result[0].length; x++) {
                        correspondences.add(new org.utils.Correspondence<>(tblA.getColumn(y).getLabel(), tblB.getColumn(x).getLabel(), result[y][x]));
                    }
                }
            }
        }
        return correspondences;
    }

    public static List<org.utils.Correspondence<String>> matchXG(String[] schemaA, String[] schemaB) {
        return new ArrayList<>();
    }
}
