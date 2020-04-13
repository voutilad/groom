package io.sisu.groom.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Actor {

    enum Type {
        PLAYER,
        SOLDIER,
        SHOTGUN_SOLDIER,
        DEMON,
        SPECTRE,
        IMP,
        IMP_FIREBALL,
        UNDEAD,
        LOST_SOUL,
        CACODEMON,
        CACODEMON_FIREBALL,
        BARON_OF_HELL,
        BARON_FIREBALL,
        BARREL,
        ROCKET,
        PLASMA,
        UNKNOWN_ENEMY
    }

    private Type type;
    private String id;
    private Optional<Position> position = Optional.empty();
    private int health;
    private int armor;

    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap();
        map.put("type", type.toString().toLowerCase());
        map.put("id", id);
        map.put("health", health);
        map.put("armor", armor);
        position.ifPresent(pos -> map.put("position", pos.toMap()));
        return map;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Optional<Position> getPosition() {
        return position;
    }

    public void setPosition(Optional<Position> position) {
        this.position = position;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = armor;
    }
}
