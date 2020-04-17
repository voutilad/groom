package io.sisu.groom.events;

import java.util.HashMap;
import java.util.Map;

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

  public Map<String, Object> toMap() {
    HashMap<String, Object> map = new HashMap();
    map.put("tic", tic);
    map.put("millis", millis);
    return map;
  }

  @Override
  public int compareTo(Frame other) {
    return this.tic - other.tic;
  }
}
