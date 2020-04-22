package io.sisu.groom.exceptions;

public class InvalidEventException extends RuntimeException {
  private final String json;

  public InvalidEventException(String message, String json) {
    super(message);
    this.json = json;
  }

  public InvalidEventException(String message) {
    this(message, "n/a");
  }

  @Override public String toString() {
    return "InvalidEventException{" +
        "json='" + json + '\'' +
        '}';
  }
}
