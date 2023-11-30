package org.converter;

import org.SimilarityFlooding.DataTypes.Graph;
import org.SimilarityFlooding.DataTypes.Relation;
import org.utils.Correspondence;
import org.yaml.snakeyaml.Yaml;
import scenarioCreator.data.identification.*;
import scenarioCreator.generation.processing.ScenarioCreator;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class YAMLParser {
    private static final HashMap<String, Node> nodeStorage = new HashMap<>();

    static {
        nodeStorage.put("schematype_table", new Node("table"));
        nodeStorage.put("schematype_column", new Node("column"));
        nodeStorage.put("schematype_columntype", new Node("columntype"));
    }

    /**
     * Parses YAML input into a graph.
     *
     * @return Graph containing Nodes and Edges
     */
    public static Optional<Graph<Node>> ParseGraph(String schema) {
        final Yaml yaml = new Yaml();
        final Map<String, Object> obj;
        try {
            obj = yaml.load(new FileInputStream(schema));
        } catch (FileNotFoundException e) {
            return Optional.empty();
        }

        final var nodes = new ArrayList<Node>();
        final var edges = new ArrayList<Relation<Node>>();

        final var tables = (ArrayList) obj.get("tableSet");
        tables.forEach(tb -> {
            final var idPath = (Map<String, Map<String, Object>>) tb;
            final var tb1 = (Map<String, Map<String, String>>) tb;
            final var tb3 = (Map<String, Map<String, ArrayList<Map<String, String>>>>) tb;
            final var tb2 = (Map<String, ArrayList<Map<String, ?>>>) tb;

            // table root
            var root = Objects.requireNonNullElse(
                    tb1.get("name").get("rawstring"),
                    tb3
                            .get("name")
                            .get("segmentList")
                            .stream().map(element -> element.get("token")).collect(Collectors.joining()));
            final Id rootId = parseId(idPath.get("id"));
            final var rootNode = new Node(root, rootId);
            nodes.add(rootNode);
            edges.add(new Relation<>("type", rootNode, nodeStorage.get("schematype_table")));

            Node leftNeighbor = null;

            for (final var tC : tb2.get("columnList")) {
                final var cId = (Map<String, Map<String, Object>>) tC;
                final var cname = (Map<String, Map<String, Object>>) tC;
                final var ctype = (Map<String, Map<String, String>>) tC;
                final var colId = parseId(cId.get("id"));

                final var colnameSegments = cname.get("name").get("segmentList");
                var colname = "";
                if (colnameSegments == null)
                    colname = ((Map<String, Map<String, String>>) tC).get("name").get("rawString");
                else
                    colname = ((Map<String, Map<String, ArrayList<Map<String, String>>>>) tC)
                            .get("name").get("segmentList").stream()
                            .map(segment -> segment.get("token"))
                            .collect(Collectors.joining());
                System.out.print("| " + colname + " ");
                if (ctype.get("dataType") == null) return;
                final var node = new Node(colname, colId);
                if (leftNeighbor != null) {
                    edges.add(new Relation<>("neighbor", leftNeighbor, node));
                }
                leftNeighbor = node;

                final var colType = "datatype_" + ctype.get("dataType").get("dataTypeEnum");
                /*final var dtype = new Node(colType);
                nodes.add(dtype);
                edges.add(new Relation<>("type", dtype, nodeStorage.get("schematype_columntype")));
                edges.add(new Relation<>("sqltype", node, dtype));*/
                var dtype = nodeStorage.get(colType);
                if (dtype == null) {
                    dtype = new Node(colType);
                    nodeStorage.put(colType, dtype);
                }
                edges.add(new Relation<>("type", dtype, nodeStorage.get("schematype_columntype")));
                edges.add(new Relation<>("sqltype", node, dtype));


                nodes.add(node);
                edges.add(new Relation<>("column", rootNode, node));
                edges.add(new Relation<>("type", node, nodeStorage.get("schematype_column")));
            }
        });
        System.out.print("\n");
        nodes.addAll(nodeStorage.values());

        return Optional.of(new Graph<>(nodes, edges));
    }

    public static List<Correspondence<Node>> ParseCorrespondences(String correspondenceFile) {
        final Yaml yaml = new Yaml();
        final ArrayList<Map<String, ?>> obj;
        try {
            obj = yaml.load(new FileInputStream(correspondenceFile));
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        }
        final var result = new ArrayList<Correspondence<Node>>();
        obj.forEach(corr -> {
            final var similarity = ((Map<String, Double>) corr).get("similarity");
            final var nodeAName = parseName(((Map<String, Map<String, Map<String, ?>>>) corr).get("nodeA").get("name"));
            final var nodeAId = parseId(((Map<String, Map<String, Map<String, Object>>>) corr).get("nodeA").get("id"));
            final var nodeBName = parseName(((Map<String, Map<String, Map<String, ?>>>) corr).get("nodeB").get("name"));
            final var nodeBId = parseId(((Map<String, Map<String, Map<String, Object>>>) corr).get("nodeB").get("id"));
            result.add(new Correspondence<>(new Node(nodeAName.toLowerCase(), nodeAId), new Node(nodeBName.toLowerCase(), nodeBId), similarity));
        });
        return result;
    }

    private static String parseName(Map<String, ?> name) {
        final var cname = (Map<String, ?>) name;

        final var colnameSegments = cname.get("segmentList");
        if (colnameSegments == null)
            return ((Map<String, String>) name).get("rawString");
        else
            return ((Map<String, ArrayList<Map<String, String>>>) name)
                    .get("segmentList").stream()
                    .map(segment -> segment.get("token"))
                    .collect(Collectors.joining());
    }

    private static Id parseId(Map<String, ?> id) {
        if (id == null) return null;

        final var simpleId = ((Map<String, Integer>) id).get("number");
        if (simpleId != null) return new IdSimple(simpleId);

        var mergeOrSplitTypeString = ((Map<String, String>) id).get("mergeType");
        if (mergeOrSplitTypeString == null) mergeOrSplitTypeString = ((Map<String, String>) id).get("splitType");
        final var mergeType = switch (mergeOrSplitTypeString) {
            case "And" -> MergeOrSplitType.And;
            case "Xor" -> MergeOrSplitType.Xor;
            default -> MergeOrSplitType.Other;
        };

        final var pred1Id = parseId(((Map<String, Map<String, Integer>>) id).get("predecessorId1"));
        if (pred1Id != null) {
            var pred2Id = parseId(((Map<String, Map<String, Integer>>) id).get("predecessorId2"));
            return new IdMerge(pred1Id, pred2Id, mergeType);
        }

        final var predId = parseId(((Map<String, Map<String, Integer>>) id).get("predecessorId"));
        final var extensionNumber = ((Map<String, Integer>) id).get("extensionNumber");

        return new IdPart(predId, extensionNumber, mergeType);
    }
}
