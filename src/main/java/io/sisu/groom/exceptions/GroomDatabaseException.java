package io.sisu.groom.exceptions;

import java.util.Optional;

/**
 * Wraps all expected errors related to connecting to and initializing the Neo4j backend database
 * used by Groom.
 */
public class GroomDatabaseException extends RuntimeException {

  public enum Problem {
    MISSING_APOC,
    SCHEMA_FAILURE,
    CONNECTION_FAILURE
  }

  private final Problem problem;

  public GroomDatabaseException(String message, Problem problem) {
    super(message);
    this.problem = problem;
  }

  public Problem getProblem() {
    return problem;
  }

  @Override
  public String toString() {
    return String.format(
        "DatabaseException{problem=%s, message=%s}",
        problem, Optional.ofNullable(getLocalizedMessage()).orElse(""));
  }
}
