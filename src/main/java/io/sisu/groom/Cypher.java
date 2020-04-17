package io.sisu.groom;

import io.sisu.groom.events.Event;
import org.neo4j.driver.Query;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Cypher {
  public static final String[] SCHEMA_QUERIES = {
    "CREATE CONSTRAINT ON (n:Mission) ASSERT n.id IS UNIQUE",
    "CREATE CONSTRAINT ON (n:Frame) ASSERT n.tic IS UNIQUE",
    "CREATE CONSTRAINT ON (n:Level) ASSERT n.id IS UNIQUE",
    "CREATE CONSTRAINT ON (n:Actor) ASSERT n.id IS UNIQUE",
    "CREATE CONSTRAINT ON (n:SubSector) ASSERT n.id IS UNIQUE",
    "CREATE INDEX ON :Frame(millis)",
    "CREATE INDEX ON :Event(counter)",
    "CREATE INDEX ON :Event(type)",
    "CREATE INDEX ON :Actor(type)",
    "CREATE INDEX ON :Enemy(type)",
    "CREATE INDEX ON :State(position)",
    "CREATE INDEX ON :PlayerState(position)",
    "CREATE INDEX ON :EnemyState(position)",
    "CREATE INDEX ON :State(actorId)",
    "CREATE INDEX ON :PlayerState(actorId)",
    "CREATE INDEX ON :EnemyState(actorId)",
  };

  private static final String UNWIND_PARAM = "eventList";
  private static final String BULK_EVENT_INSERT_CYPHER =
      String.join(
          "\n",
          new String[] {
            // Bulk construct event components
            "UNWIND $" + UNWIND_PARAM + " AS event",

            // COMMON Event/Frame Logic
            "MERGE (frame:Frame {tic: event.frame.tic}) ON CREATE SET frame.millis = event.frame.millis",
            "CREATE (ev:Event {type: event.type, counter: event.counter})",
            "CREATE (ev)-[:OCCURRED_AT]->(frame)",

            // Conditionally process Actor and Target",
            "FOREACH (thing IN [x IN [event.actor, event.target] WHERE x IS NOT NULL] |",
            "    MERGE (actor:Actor {id: thing.id}) ON CREATE SET actor.type = thing.type",
            "    MERGE (subsector:SubSector {id: thing.position.subsector})",
            "    CREATE (actorState:State)",
            "    SET actorState.position = point(thing.position),",
            "        actorState.angle = thing.position.angle,",
            "        actorState.health = thing.health,",
            "        actorState.armor = thing.armor,",
            "        actorState.actorId = thing.id",
            "    CREATE (actorState)-[:IN_SUBSECTOR]->(subsector)",

            // Hacky logic...hold your nose
            "    FOREACH (_ IN CASE thing.id WHEN event.actor.id THEN [1] ELSE [] END | CREATE (actorState)-[:ACTOR_IN]->(ev))",
            "    FOREACH (_ IN CASE thing.id WHEN event.target.id THEN [1] ELSE [] END | CREATE (actorState)-[:TARGET_IN]->(ev))",
            "    FOREACH (_ IN CASE thing.type WHEN 'player' THEN [1] ELSE [] END | SET actor:Player, actorState:PlayerState)",
            "    FOREACH (_ IN CASE thing.type WHEN 'player' THEN [] ELSE [1] END | SET actor:Enemy, actorState:EnemyState)",
            ")",
          });

  public static final String THREAD_FRAMES =
      String.join(
          "\n",
          "MATCH (f:Frame) WHERE NOT (f)<-[:PREV_FRAME]-()",
          "WITH f ORDER BY f.tic",
          "WITH collect(f) AS frames",
          "UNWIND apoc.coll.pairsMin(frames) AS pair",
          "WITH pair[0] AS prev, pair[1] AS next",
          "    CREATE (next)-[:PREV_FRAME]->(prev)");

  public static final String THREAD_EVENTS =
      String.join(
          "\n",
          "MATCH (e:Event) WHERE NOT (e)<-[:PREV_EVENT]-()",
          "WITH e ORDER BY e.counter",
          "WITH collect(e) AS events",
          "UNWIND apoc.coll.pairsMin(events) AS pair",
          "WITH pair[0] AS prev, pair[1] AS next",
          "    CREATE (next)-[:PREV_EVENT]->(prev)");

  public static final String THREAD_STATES =
      String.join(
          "\n",
          "MATCH (a:Actor)",
          "MATCH (s:State {actorId:a.id})-[:ACTOR_IN|:TARGET_IN]->(e:Event)",
          "    WHERE NOT (s)<-[:PREV_STATE]-()",
          "WITH s, e ORDER BY e.counter",
          "WITH collect(s) AS states, s.actorId AS actorId",
          "UNWIND apoc.coll.pairsMin(states) AS pair",
          "WITH pair[0] AS prev, pair[1] AS next",
          "    CREATE (next)-[:PREV_STATE]->(prev)");

  public static final String CURRENT_STATE_DELETE =
      "MATCH (a:Actor)-[r:CURRENT_STATE]->(old:State) DELETE r";

  public static final String CURRENT_STATE_UPDATE =
      String.join(
          "\n",
          "MATCH (s:State) WHERE NOT (s)<-[:PREV_STATE]-()",
          "MATCH (a:Actor {id:s.actorId})",
          "MERGE (a)-[:CURRENT_STATE]->(s)");

  public static Mono<BulkQuery> compileBulkEventComponentInsert(List<Event> events) {
    if (events == null || events.isEmpty()) {
      return Mono.empty();
    }
    Map<String, Object> params = new HashMap();
    params.put(UNWIND_PARAM, events.stream().map(Event::toMap).collect(Collectors.toList()));

    return Mono.just(new BulkQuery(events.size(), new Query(BULK_EVENT_INSERT_CYPHER, params)));
  }
}
