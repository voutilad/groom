package io.sisu.groom.events;

public class Position {
    int x, y, z;

    // angle_t is an unsigned int in Doom like 3,221,225,472 which will overflow a signed 4 byte int
    long angle;

    int subsector;

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

    public int getSubsector() {
        return subsector;
    }

    public void setSubsector(int subsector) {
        this.subsector = subsector;
    }
}
