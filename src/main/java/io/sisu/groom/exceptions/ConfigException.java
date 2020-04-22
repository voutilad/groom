package io.sisu.groom.exceptions;

import java.util.Optional;

public class ConfigException extends RuntimeException {

  public enum Problem {
    ASKED_FOR_HELP,
    INVALID_VALUE,
    UNHANDLED_OPTION
  }

  private final Problem problem;

  public ConfigException(String message, Problem problem) {
    super(message);
    this.problem = problem;
  }

  @Override
  public String toString() {
    return String.format(
        "ConfigException{problem=%s, message=%s}",
        problem, Optional.ofNullable(getLocalizedMessage()).orElse(""));
  }
}
