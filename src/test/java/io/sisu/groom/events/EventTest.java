package io.sisu.groom.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class EventTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new Jdk8Module());

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
        Optional<Event> event = Event.fromJson("{\"angle\": 1234}").blockOptional();
        Assertions.assertFalse(event.isPresent());
    }

    @Test
    void frameCannotBeNull() {
        Optional<Event> event = Event.fromJson("{\"type\": \"MOVE\"}").blockOptional();
        Assertions.assertFalse(event.isPresent());
    }

    @Test
    void caseInsensitiveEnumDeserialization() {
        Mono<Event> event = Event.fromJson("{\"type\": \"MOVE\", \"frame\":{}, \"actor\":{\"type\":\"shotgun_soldier\"}}");
        Optional<Event> maybeEvent = event.blockOptional();
        Assertions.assertTrue(maybeEvent.isPresent());
        Assertions.assertEquals(Actor.Type.SHOTGUN_SOLDIER, maybeEvent.get().getActor().get().getType());
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
}
