package io.sisu.groom;

public class Cypher {
    public static final String[] SCHEMA_QUERIES = {
            // Primary Keys
            "CREATE CONSTRAINT ON (n:Mission) ASSERT n.id IS UNIQUE",
            "CREATE CONSTRAINT ON (n:Frame) ASSERT n.id IS UNIQUE",
            "CREATE CONSTRAINT ON (n:Level) ASSERT n.id IS UNIQUE",
            "CREATE CONSTRAINT ON (n:Enemy) ASSERT n.id IS UNIQUE",
            "CREATE CONSTRAINT ON (n:Sector) ASSERT n.id IS UNIQUE",

            // Node Properties Must Exist
            "CREATE CONSTRAINT ON (n:Frame) ASSERT exists(n.tic)",
            //"CREATE CONSTRAINT ON (n:Event) ASSERT exists(n.position)",
            "CREATE CONSTRAINT ON (n:Event) ASSERT exists(n.type)",

            // Indexes
            "CREATE INDEX ON :Frame(millis)",
            "CREATE INDEX ON :Frame(tic)",
            "CREATE INDEX ON :Enemy(type)",
    };
}
