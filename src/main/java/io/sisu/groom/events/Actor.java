package io.sisu.groom.events;

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
    private Optional<Position> position;

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
}
