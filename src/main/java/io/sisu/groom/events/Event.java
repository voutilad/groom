package io.sisu.groom.events;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Event {

    public enum Type {
        START_LEVEL,
        END_LEVEL,
        TARGETED,
        KILLED,
        ATTACKED,
        COUNTER_ATTACKED,
        HIT,
        PICKUP_ARMOR,
        PICKUP_HEALTH,
        PICKUP_WEAPON,
        PICKUP_CARD,
        ARMOR_BONUS,
        HEALTH_BONUS,
        ENTER_SECTOR,
        ENTER_SUBSECTOR,
        MOVE;
    }

    private Type type;
    private Frame frame;

    private Actor actor;
    private Optional<Actor> target = Optional.empty();
    private Optional<Level> level = Optional.empty();

    // Not really doing anything with these yet...
    int health;
    int card;
    int damage;
    int armor;
    int weaponType;
    int armorType;

    private static final Logger logger = LoggerFactory.getLogger(Event.class);
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy());

    public static Mono<Event> fromJson(String json) {
        Event event;
        try {
            event = mapper.readValue(json, Event.class);
            if (event.type == null) {
                throw new Exception("event type should not be null");
            }
            if (event.frame == null) {
                throw new Exception("frame cannot be null!");
            }
            if (event.actor == null) {
                throw new Exception("actor cannot be null!");
            }
        } catch (Exception e) {
            logger.error("Could not parse json: " + e.getMessage() + " --> " + json);
            event = null;
        }
        return Mono.justOrEmpty(event);
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap();
        map.put("type", type.toString());
        map.put("frame", frame.toMap());
        map.put("actor", actor.toMap());
        level.ifPresent(l -> map.put("level", l.toMap()));
        target.ifPresent(t -> map.put("target", t.toMap()));
        return map;
    }

    @Override
    public String toString() {
        return String.format("Event[%s@%d]", type, frame.getTic());
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Frame getFrame() {
        return frame;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }

    public Actor getActor() {
        return actor;
    }

    public void setActor(Actor actor) {
        this.actor = actor;
    }

    public Optional<Actor> getTarget() {
        return target;
    }

    public void setTarget(Optional<Actor> target) {
        this.target = target;
    }


    public Optional<Level> getLevel() {
        return level;
    }

    public void setLevel(Optional<Level> level) {
        this.level = level;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getCard() {
        return card;
    }

    public void setCard(int card) {
        this.card = card;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = armor;
    }

    public int getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(int weaponType) {
        this.weaponType = weaponType;
    }

    public int getArmorType() {
        return armorType;
    }

    public void setArmorType(int armorType) {
        this.armorType = armorType;
    }
}
