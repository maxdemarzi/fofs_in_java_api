package org.neo4j.fofs;

import org.codehaus.jackson.annotate.JsonValue;

public  final class MutableInt implements Comparable<MutableInt> {
    int value = 0;

    public MutableInt (int value){
        this.value = value;
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    public void increment() {
        ++value;
    }

    @Override
    public int compareTo(MutableInt o) {
        return value - o.value;
    }
}
