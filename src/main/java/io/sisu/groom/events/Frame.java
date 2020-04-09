package io.sisu.groom.events;

public class Frame implements Comparable<Frame> {
    private int millis;
    private int tic;

    public int getMillis() {
        return millis;
    }

    public void setMillis(int millis) {
        this.millis = millis;
    }

    public int getTic() {
        return tic;
    }

    public void setTic(int tic) {
        this.tic = tic;
    }

    @Override
    public int compareTo(Frame other) {
        return this.tic - other.tic;
    }
}
