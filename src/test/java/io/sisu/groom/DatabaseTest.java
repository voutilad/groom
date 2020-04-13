package io.sisu.groom;

import io.sisu.groom.events.Event;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Query;

import java.util.*;
import java.util.stream.Collectors;

public class DatabaseTest {

    public static final String json1 = "{\"type\":\"move\",\"frame\":{\"millis\":40200,\"tic\":1407},\"actor\":{\"position\":{\"x\":-185472,\"y\":27469568,\"z\":0,\"angle\":1073741824,\"subsector\":4350048792},\"type\":\"shotgun_soldier\",\"health\":30,\"id\":4350211784}}";
    public static final String json2 = "{\"type\":\"move\",\"frame\":{\"millis\":40200,\"tic\":1407},\"actor\":{\"position\":{\"x\":50632464,\"y\":10573328,\"z\":0,\"angle\":2147483648,\"subsector\":4350049784},\"type\":\"spectre\",\"health\":150,\"id\":4350222344}}";
    public static final String json3 = "{\"type\":\"move\",\"frame\":{\"millis\":40200,\"tic\":1407},\"actor\":{\"position\":{\"x\":3052973,\"y\":7851589,\"z\":2097152,\"angle\":3331082136,\"subsector\":4350048536},\"type\":\"imp_fireball\",\"health\":1000,\"id\":4350746312}}";
    public static final String json4 = "{\"type\":\"move\",\"frame\":{\"millis\":40229,\"tic\":1408},\"actor\":{\"position\":{\"x\":4141444,\"y\":-37142,\"z\":0,\"angle\":1275068416,\"subsector\":4350048376},\"type\":\"player\",\"health\":36,\"armor\":53,\"id\":4350176240}}";

    @Test
    void params() {
        Database db = new Database(Database.defaultConfig, "neo4j", "password");
        List<Event> events = Arrays.asList(json1, json2, json3, json4)
                .stream()
                .map(json -> Event.fromJson(json).block())
                .collect(Collectors.toList());
        Query q = Cypher.compileBulkEventComponentInsert(events);
        System.out.println(q.text());
        db.write(q).blockLast();
    }
}