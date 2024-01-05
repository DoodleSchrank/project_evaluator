package org.converter;

import org.SimilarityFlooding.DataTypes.Graph;
import org.SimilarityFlooding.DataTypes.Relation;
import org.types.Node;
import org.yaml.snakeyaml.Yaml;
import scenarioCreator.data.identification.*;

import java.io.*;
import java.util.*;

public class GraphParser extends Parser {
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
    public static Optional<Graph<Node>> Parse(String schemaURL, Boolean linkedSF) {
        final Yaml yaml = new Yaml();
        final Map<String, Object> obj;
        try {
            obj = yaml.load(new FileInputStream(schemaURL));
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
            final var rootName = ParseName(tb1.get("name"));

            final Id rootId = ParseId(idPath.get("id"));
            final var rootNode = new Node(rootName, rootId);
            nodes.add(rootNode);
            edges.add(new Relation<>("type", rootNode, nodeStorage.get("schematype_table")));

            Node leftNeighbor = null;

            for (final var tC : tb2.get("columnList")) {
                final var cId = (Map<String, Map<String, Object>>) tC;
                final var cname = (Map<String, Map<String, Object>>) tC;
                final var ctype = (Map<String, Map<String, String>>) tC;
                final var colId = ParseId(cId.get("id"));

                final var colName = ParseName(cname.get("name"));
                if (ctype.get("dataType") == null) return;
                final var node = new Node(colName, colId);
                if (linkedSF) {
                    if (leftNeighbor != null) {
                        edges.add(new Relation<>("neighbor", leftNeighbor, node));
                    }
                    leftNeighbor = node;
                }

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
        nodes.addAll(nodeStorage.values());

        return Optional.of(new Graph<>(nodes, edges));
    }
}