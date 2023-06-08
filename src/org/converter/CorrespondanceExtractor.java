package org.converter;

import org.SimilarityFlooding.DataTypes.Graph;
import org.SimilarityFlooding.DataTypes.Relation;
import org.utils.Correspondence;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

public class CorrespondanceExtractor {
    public static List<Correspondence<?>> convert(String file) {
        return new ArrayList<>();
    }

    public static Optional<List<Correspondence<String>>> ParseCorrs(String corrFile) {
        Yaml yaml = new Yaml();
        ArrayList<Map<String, Map<String, Map<String, ArrayList<Map<String, String>>>>>> obj;
        try {
            obj = yaml.load(new FileInputStream(corrFile));
        } catch (FileNotFoundException e) {
            return Optional.empty();
        }
        System.out.println(obj.get(0).get("nodeA").get("name").get("segmentList").get(0).get("token"));
        var corrs = obj.stream().map(corr -> new Correspondence<>(
                corr.get("nodeA").get("name").get("segmentList").get(0).get("token"),
                corr.get("nodeB").get("name").get("segmentList").get(0).get("token"),
                1.0f)).toList();
        /*tables.forEach(tb -> {
            var tb1 = (Map<String, Map<String, ArrayList<Map<String, String>>>>) tb;
            var tb2 = (Map<String, ArrayList<Map<String, ?>>>) tb;

            // table root
            var root = tb1
                    .get("name")
                    .get("segmentList")
                    .get(1)
                    .get("token");
            nodes.add(root);

            tb2.get("columnList").forEach(tC -> {
                var cname = (Map<String, Map<String, ArrayList<Map<String, String>>>>) tC;
                var ctype = (Map<String, Map<String, String>>) tC;
                var colnode = cname.get("name").get("segmentList").get(1).get("token");
                var colType = ctype.get("dataType").get("dataTypeEnum");

                datatypes.add(colType);

                nodes.add(colnode);
                edges.add(new Relation<>("column", root, colnode));
                edges.add(new Relation<>("type", colnode, schematypes.get(1)));
                edges.add(new Relation<>("sqltype", colnode, datatypes.stream()
                        .filter(dt -> dt.equals(colType))
                        .toList().get(0)));
            });
        });*/

        return Optional.of(new ArrayList<Correspondence<String>>());
    }
}
