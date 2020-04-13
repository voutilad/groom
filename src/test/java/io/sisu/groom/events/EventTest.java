package io.sisu.groom.events;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class EventTest {

    @Test
    void deserializeJSON() throws Exception {
        String json = "{\"type\":\"move\",\"frame\":{\"millis\":109029,\"tic\":3816},\"actor\":{\"position\":{\"x\":-82259968,\"y\":29911040,\"z\":-11534336,\"angle\":3221225472,\"subsector\":49503504},\"type\":\"imp\",\"id\":49640152}}\n";
        Mono<Event> maybeEvent = Event.fromJson(json);
        Optional<Event> event = maybeEvent.blockOptional();
        Assertions.assertTrue(event.isPresent());
        Assertions.assertEquals(Event.Type.MOVE, event.get().getType());
    }

    @Test
    void typeCannotBeNull() {
        Optional<Event> event = Event.fromJson("{\"frame\": {\"tic\": 1, \"millis\": 1}}").blockOptional();
        Assertions.assertFalse(event.isPresent());
    }

    @Test
    void frameCannotBeNull() {
        Optional<Event> event = Event.fromJson("{\"type\": \"MOVE\", \"actor\": {\"id\": 1, \"type\": \"imp\"}}").blockOptional();

        Assertions.assertFalse(event.isPresent());
    }

    @Test
    void actorCannotBeNull() {
        Optional<Event> event = Event.fromJson("{\"type\": \"MOVE\", \"frame\": {\"tic\": 1, \"millis\": 1}}").blockOptional();
        Assertions.assertFalse(event.isPresent());
    }

    @Test
    void caseInsensitiveEnumDeserialization() {
        Mono<Event> event = Event.fromJson("{\"type\": \"MOVE\", \"frame\":{}, \"actor\":{\"type\":\"shotgun_soldier\"}}");
        Optional<Event> maybeEvent = event.blockOptional();
        Assertions.assertTrue(maybeEvent.isPresent());
        Assertions.assertEquals(Actor.Type.SHOTGUN_SOLDIER, maybeEvent.get().getActor().getType());
    }

    @Test
    void snakeCaseSupport() {
        Mono<Event> event = Event.fromJson("{\"type\": \"PICKUP_WEAPON\", \"weapon_type\":3, \"frame\":{}, \"actor\":{\"type\":\"shotgun_soldier\"}}");
        Optional<Event> maybeEvent = event.blockOptional();
        Assertions.assertTrue(maybeEvent.isPresent());
        Assertions.assertEquals(3, maybeEvent.get().getWeaponType());
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
        Mono<Event> event = Event.fromJson("{\"type\":\"move\",\"frame\":{\"millis\":40200,\"tic\":1407},\"actor\":{\"position\":{\"x\":-185472,\"y\":27469568,\"z\":0,\"angle\":1073741824,\"subsector\":4350048792},\"type\":\"shotgun_soldier\",\"health\":30,\"id\":4350211784}}\n");
        Assertions.assertNotNull(event.block().getTarget());
        Assertions.assertFalse(event.block().getTarget().isPresent());
    }
}
