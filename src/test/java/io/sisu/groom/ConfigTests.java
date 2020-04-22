package io.sisu.groom;

import io.sisu.groom.exceptions.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfigTests {

  @Test
  public void validateWeDealWithArgparsesSillyUnderscores() {
    String[] args =
        new String[] {
          "--" + Config.KEY_UDP_PORT, "12345",
          "--" + Config.KEY_UDP_HOST, "localhost",
          "--" + Config.KEY_BATCH_SIZE, "999",
          "--" + Config.KEY_WINDOW_TIMEOUT, "99",
        };
    Config config = new Config(args);
    Assertions.assertEquals(config.udpHost, "localhost");
    Assertions.assertEquals(config.udpPort, 12345);
    Assertions.assertEquals(config.bufferSize, 999);
    Assertions.assertEquals(config.flushInterval, 99);
  }

  @Test
  public void throwErrorsOnReallyBadIdeasForSettings() {
    Assertions.assertThrows(
        ConfigException.class,
        () -> new Config(new String[] {"--" + Config.KEY_BATCH_SIZE, "1"}));
    Assertions.assertThrows(
        ConfigException.class,
        () -> new Config(new String[] {"--" + Config.KEY_WINDOW_TIMEOUT, "-1"}));
  }
}
