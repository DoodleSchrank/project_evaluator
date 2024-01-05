package org.converter;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Table;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

public class SchematcherParser extends Parser {
    /**
     * Parses YAML input into a graph.
     *
     * @return Graph containing Nodes and Edges
     */
    public static Optional<Database> Parse(String schemaURL) {

        final Yaml yaml = new Yaml();
        final Map<String, Object> obj;
        try {
            obj = yaml.load(new FileInputStream(schemaURL));
        } catch (FileNotFoundException e) {
            return Optional.empty();
        }
        final var tableMap = new HashMap<String, Table>();
        final var tables = (ArrayList) obj.get("tableSet");
        tables.forEach(tb -> {
            final var tb1 = (Map<String, Map<String, String>>) tb;
            final var tb2 = (Map<String, ArrayList<Map<String, ?>>>) tb;

            // table root
            final var tableRootName = ParseName(tb1.get("name"));
            final var labels = new ArrayList<String>();
            final var columns = new ArrayList<Column>();
            for (final var tC : tb2.get("columnList")) {
                final var cname = (Map<String, Map<String, Object>>) tC;
                final var colName = ParseName(cname.get("name"));
                labels.add(colName);
                columns.add(new Column(colName, new ArrayList<>()));
            }
            tableMap.put(tableRootName, new Table(tableRootName, labels, columns));
        });
        final var db = new Database(ParseName((Map<String, ?>) obj.get("name")), tableMap);

        return Optional.of(db);
    }
}
