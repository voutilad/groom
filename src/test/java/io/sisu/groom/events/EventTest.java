package io.sisu.groom.events;

import io.sisu.groom.exceptions.InvalidEventException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventTest {

  @Test
  void deserializeJSON() {
    String json =
        "{\"session\": \"abc\", \"counter\": 0, \"type\":\"targeted\",\"frame\":{\"millis\":40458,\"tic\":1416},\"actor\":{\"position\":{\"x\":3119532,\"y\":-1133745,\"z\":0,\"angle\":1006632960,\"subsector\":4350048376},\"type\":\"player\",\"health\":36,\"armor\":56,\"id\":4350176240},\"target\":{\"type\":\"barrel\",\"health\":5,\"id\":4350225776}}";
    Event event = Event.fromJson(json);

    Assertions.assertNotNull(event);
    Assertions.assertEquals(Event.Type.TARGETED, event.getType());
    Assertions.assertNotNull(event.getActor());
    Assertions.assertTrue(event.getTarget().isPresent());

    Frame frame = event.getFrame();
    Assertions.assertEquals(1416, frame.getTic());
    Assertions.assertEquals(40458, frame.getMillis());

    Actor actor = event.getActor();
    Assertions.assertEquals(Actor.Type.PLAYER, actor.getType());
    Assertions.assertEquals("4350176240", actor.getId());

    Actor target = event.getTarget().get();
    Assertions.assertEquals(Actor.Type.BARREL, target.getType());
    Assertions.assertEquals("4350225776", target.getId());

    Assertions.assertTrue(actor.getPosition().isPresent());
    Position pos = actor.getPosition().get();
    Assertions.assertNotNull(pos);
    Assertions.assertEquals(4350048376L, pos.getSubsector());
  }

  @Test
  void deserializeProblemJson() {
    final String json =
        "{\"session\": \"abc\", \"counter\": 0,\"type\":\"targeted\",\"frame\":{\"millis\":7315,\"tic\":256},\"actor\":{\"position\":{\"x\":-4194304,\"y\":16777216,\"z\":0,\"angle\":3221225472,\"subsector\":4350048536},\"type\":\"spectre\",\"health\":150,\"id\":4350217592},\"target\":{\"type\":\"player\",\"health\":100,\"armor\":0,\"id\":4350176240}}";
    Event event = Event.fromJson(json);

    Assertions.assertNotNull(event);
    Assertions.assertEquals(Event.Type.TARGETED, event.getType());
    Assertions.assertNotNull(event.getActor());
    Assertions.assertTrue(event.getTarget().isPresent());

    Frame frame = event.getFrame();
    Assertions.assertEquals(256, frame.getTic());
    Assertions.assertEquals(7315, frame.getMillis());

    Actor actor = event.getActor();
    Assertions.assertEquals(Actor.Type.SPECTRE, actor.getType());
    Assertions.assertEquals("4350217592", actor.getId());

    Actor target = event.getTarget().get();
    Assertions.assertEquals(Actor.Type.PLAYER, target.getType());
    Assertions.assertEquals("4350176240", target.getId());

    Assertions.assertTrue(actor.getPosition().isPresent());
    Position pos = actor.getPosition().get();
    Assertions.assertNotNull(pos);
    Assertions.assertEquals(4350048536L, pos.getSubsector());
  }

  @Test
  void counterCannotBeNull() {
    Assertions.assertThrows(InvalidEventException.class, () ->
        Event.fromJson(
                "{\"session\": \"abc\", \"type\": \"PICKUP_WEAPON\", \"weapon_type\":3, \"frame\":{}, \"actor\":{\"type\":\"shotgun_soldier\", \"position\":{}}}"));
  }

  @Test
  void typeCannotBeNull() {
    Assertions.assertThrows(InvalidEventException.class, () ->
        Event.fromJson("{\"session\": \"abc\", \"counter\": 0,\"frame\": {\"tic\": 1, \"millis\": 1}}"));
  }

  @Test
  void frameCannotBeNull() {
    Assertions.assertThrows(InvalidEventException.class, () ->
        Event.fromJson(
                "{\"session\": \"abc\", \"counter\": 0,\"type\": \"MOVE\", \"actor\": {\"id\": 1, \"type\": \"imp\"}}"));
  }

  @Test
  void actorCannotBeNull() {
    Assertions.assertThrows(InvalidEventException.class, () ->
        Event.fromJson(
                "{\"session\": \"abc\", \"counter\": 0, \"type\": \"MOVE\", \"frame\": {\"tic\": 1, \"millis\": 1}}"));
  }

  @Test
  void caseInsensitiveEnumDeserialization() {
    Event event =
        Event.fromJson(
            "{\"session\": \"abc\", \"counter\": 0, \"type\": \"MOVE\", \"frame\":{}, \"actor\":{\"type\":\"shotgun_soldier\", \"position\":{}}}");
    Assertions.assertEquals(Actor.Type.SHOTGUN_SOLDIER, event.getActor().getType());
  }

  @Test
  void snakeCaseSupport() {
    Event event =
        Event.fromJson(
            "{\"session\": \"abc\", \"counter\": 0, \"type\": \"PICKUP_WEAPON\", \"weapon_type\":3, \"frame\":{}, \"actor\":{\"type\":\"shotgun_soldier\", \"position\":{}}}");
    Assertions.assertEquals(3, event.getWeaponType());
  }

  @Test
  void frameComparisons() {
    Frame f1 = new Frame();
    f1.setTic(30);
    Frame f2 = new Frame();
    f2.setTic(50);
    Assertions.assertTrue(f1.compareTo(f2) < 0);
    Assertions.assertTrue(f1.compareTo(f1) == 0);
    Assertions.assertTrue(f2.compareTo(f1) > 0);
  }

  @Test
  void missingTargetResultsInEmptyOptional() {
    Event event =
        Event.fromJson(
            "{\"session\": \"abc\", \"counter\": 0, \"type\":\"move\",\"frame\":{\"millis\":40200,\"tic\":1407},\"actor\":{\"position\":{\"x\":-185472,\"y\":27469568,\"z\":0,\"angle\":1073741824,\"subsector\":4350048792},\"type\":\"shotgun_soldier\",\"health\":30,\"id\":4350211784}}\n");
    Assertions.assertNotNull(event.getTarget());
    Assertions.assertFalse(event.getTarget().isPresent());
  }
}
