package org.converter;

import scenarioCreator.data.identification.Id;
import scenarioCreator.data.identification.IdSimple;

public class Node {
    private String name;
    private Id id;

    public Node(String name, Id id) {
        this.name = name;
        this.id = id;
    }

    public Node(String name) {
        this(name, new IdSimple(-1));
    }

    public Node(String name, int id) {
        this(name, new IdSimple(id));
    }

    public String toString() {
        return name;
    }


    /*
    This ugly stuff is for dumb legacy YAML Parsers that haven't been updated in 10 years.
     */
    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }
    public Id getId() { return id; }
    public void setId(Id id) { this.id = id; }
}
