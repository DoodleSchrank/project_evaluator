package org.converter;

import org.types.Node;
import org.utils.Correspondence;
import org.yaml.snakeyaml.Yaml;
import scenarioCreator.data.identification.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Parser {
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
            final var nodeAName = ParseName(((Map<String, Map<String, Map<String, ?>>>) corr).get("nodeA").get("name"));
            final var nodeAId = ParseId(((Map<String, Map<String, Map<String, Object>>>) corr).get("nodeA").get("id"));
            final var nodeBName = ParseName(((Map<String, Map<String, Map<String, ?>>>) corr).get("nodeB").get("name"));
            final var nodeBId = ParseId(((Map<String, Map<String, Map<String, Object>>>) corr).get("nodeB").get("id"));
            result.add(new Correspondence<>(new Node(nodeAName.toLowerCase(), nodeAId), new Node(nodeBName.toLowerCase(), nodeBId), similarity));
        });
        return result;
    }

    protected static String ParseName(Map<String, ?> name) {
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

    protected static Id ParseId(Map<String, ?> id) {
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

        final var pred1Id = ParseId(((Map<String, Map<String, Integer>>) id).get("predecessorId1"));
        if (pred1Id != null) {
            var pred2Id = ParseId(((Map<String, Map<String, Integer>>) id).get("predecessorId2"));
            return new IdMerge(pred1Id, pred2Id, mergeType);
        }

        final var predId = ParseId(((Map<String, Map<String, Integer>>) id).get("predecessorId"));
        final var extensionNumber = ((Map<String, Integer>) id).get("extensionNumber");

        return new IdPart(predId, extensionNumber, mergeType);
    }
}
