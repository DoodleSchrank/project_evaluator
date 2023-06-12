package org.converter;

import org.SimilarityFlooding.DataTypes.Graph;
import org.SimilarityFlooding.DataTypes.Relation;
import org.utils.Correspondence;
import org.yaml.snakeyaml.Yaml;
import scenarioCreator.data.identification.Id;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class YAMLParser {
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
        final var schematypes = new ArrayList<>(
                Arrays.asList(
                        new Node("table"),
                        new Node("column"),
                        new Node("columntype")));


        final var nodes = new ArrayList<>(schematypes);
        final var edges = new ArrayList<Relation<Node>>();
        final var datatypes = new ArrayList<Node>();


        final var tables = (ArrayList) obj.get("tableSet");
        tables.forEach(tb -> {
            final var idPath = (Map<String, Id>) tb;
            final var tb1 = (Map<String, Map<String, ArrayList<Map<String, String>>>>) tb;
            final var tb2 = (Map<String, ArrayList<Map<String, ?>>>) tb;

            // table root
            final var root = tb1
                    .get("name")
                    .get("segmentList")
                    .stream().map(element -> element.get("token")).collect(Collectors.joining());
            final Id rootId = idPath.get("id");
            final var rootNode = new Node(root, rootId);
            nodes.add(rootNode);

            tb2.get("columnList").forEach(tC -> {
                final var cId = (Map<String, Id>) tb;
                final var cname = (Map<String, Map<String, ArrayList<Map<String, String>>>>) tC;
                final var ctype = (Map<String, Map<String, String>>) tC;
                final var colId = cId.get("id");
                final var colnode = cname.get("name").get("segmentList").get(1).get("token");
                final var colType = ctype.get("dataType").get("dataTypeEnum");
                final var node = new Node(colnode, colId);

                datatypes.add(new Node(colType));

                nodes.add(node);
                edges.add(new Relation<>("column", rootNode, node));
                edges.add(new Relation<>("type", node, schematypes.get(1)));
                edges.add(new Relation<>("sqltype", node, datatypes.stream()
                        .filter(dt -> dt.toString().equals(colType))
                        .toList().get(0)));
            });
        });

        return Optional.of(new Graph<>(nodes, edges));
    }

    public static List<Correspondence<Node>> ParseCorrespondences(String correspondenceFile) {
        final Yaml yaml = new Yaml();
        final ArrayList<Correspondence<Node>> obj;
        try {
            obj = yaml.load(new FileInputStream(correspondenceFile));
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        }

        return (ArrayList<Correspondence<Node>>) obj;
    }
}
