package io.sisu.util;

import org.neo4j.driver.Query;

/**
 * Simple datatype for easily conveying how many "updates" are contained in a Query.
 */
public class BulkQuery {
  public int size;
  public Query query;

  public BulkQuery(int size, Query query) {
    this.size = size;
    this.query = query;
  }
}
