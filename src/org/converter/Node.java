package org.converter;

import scenarioCreator.data.identification.Id;
import scenarioCreator.data.identification.IdSimple;

public record Node(String name, Id id) {
    public Node(String name) {
        this(name, new IdSimple(-1));
    }
    public Node(String name, int id) {
        this(name, new IdSimple(id));
    }
    public String toString() {
        return name;
    }
}
