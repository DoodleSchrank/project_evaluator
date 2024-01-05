package org.types;

import org.utils.Correspondence;
import scenarioCreator.data.identification.Id;
import scenarioCreator.data.identification.IdSimple;

public record Node(String name, Id id) {
    public Node(String name) {
        this(name, new IdSimple(-1));
    }

    public Node(String name, int id) {
        this(name, new IdSimple(id));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Node s)) return false;
        return this.hashCode() == s.hashCode();
    }
    @Override
    public int hashCode() {
        return name.hashCode() + id.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
