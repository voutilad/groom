package io.sisu.groom.events;

import java.util.HashMap;
import java.util.Map;

public class Position {
    int x, y, z;

    // angle_t is an unsigned int in Doom like 3,221,225,472 which will overflow a signed 4 byte int
    long angle;

    long subsector;

    public Map<String, Object> toMap() {
        HashMap<String, Object> map = new HashMap();
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("angle", angle);
        map.put("subsector", subsector);
        return map;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public long getAngle() {
        return angle;
    }

    public void setAngle(long angle) {
        this.angle = angle;
    }

    public long getSubsector() {
        return subsector;
    }

    public void setSubsector(long subsector) {
        this.subsector = subsector;
    }
}
