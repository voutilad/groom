package io.sisu.groom.events;

import java.util.HashMap;
import java.util.Map;

public class Level {
  int episode;
  int level;
  int difficulty;

  public Map<String, Object> toMap() {
    HashMap<String, Object> map = new HashMap();
    map.put("episode", episode);
    map.put("level", level);
    map.put("difficulty", difficulty);
    return map;
  }

  public int getEpisode() {
    return episode;
  }

  public void setEpisode(int episode) {
    this.episode = episode;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public int getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(int difficulty) {
    this.difficulty = difficulty;
  }
}
