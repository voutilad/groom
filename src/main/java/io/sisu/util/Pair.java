package io.sisu.util;

/**
 * I really hate Java sometimes, folks.
 *
 * @param <T>
 * @param <U>
 */
public class Pair<T, U> {
  public final T a;
  public final U b;

  public Pair(T a, U b) {
    this.a = a;
    this.b = b;
  }
}
