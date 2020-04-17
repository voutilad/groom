package io.sisu.groom;

import org.neo4j.driver.Query;

public class BulkQuery {
  public int size = 0;
  public Query query;
  BulkQuery(int size, Query query) {
    this.size = size;
    this.query = query;
  }
}
