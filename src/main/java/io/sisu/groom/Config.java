package io.sisu.groom;

import io.sisu.groom.exceptions.ConfigException;
import io.sisu.groom.exceptions.ConfigException.Problem;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {
  private static final Logger logger = LoggerFactory.getLogger(Config.class);

  protected static final String KEY_BOLT_URI = "uri";
  protected static final String DEFAULT_BOLT_URI = "bolt://localhost:7687";

  protected static final String KEY_USERNAME = "user";
  protected static final String DEFAULT_USERNAME = "neo4j";

  protected static final String KEY_PASSWORD = "password";
  protected static final String DEFAULT_PASSWORD = "password";

  protected static final String KEY_USE_ENCRYPTION = "tls";
  protected static final boolean DEFAULT_USE_ENCRYPTION = true;

  protected static final String KEY_UDP_HOST = "udp-host";
  protected static final String DEFAULT_UDP_HOST = "127.0.0.1";

  protected static final String KEY_UDP_PORT = "udp-port";
  protected static final int DEFAULT_UDP_PORT = 10666;

  protected static final String KEY_BATCH_SIZE = "buffer-size";
  protected static final int DEFAULT_BATCH_SIZE = 5000;

  protected static final String KEY_WINDOW_TIMEOUT = "flush-interval";
  protected static final int DEFAULT_FLUSH_INTERVAL = 5;

  private static final ArgumentParser parser = ArgumentParsers.newFor("groom").build();

  static {
    ArgumentGroup hostGroup = parser.addArgumentGroup("Neo4j Host Settings");
    hostGroup
        .addArgument("--" + KEY_BOLT_URI)
        .help("Bolt URI to target Neo4j database (will override TLS setting)")
        .setDefault(Config.DEFAULT_BOLT_URI);
    hostGroup.addArgument("--" + KEY_USERNAME).setDefault(DEFAULT_USERNAME);
    hostGroup.addArgument("--" + KEY_PASSWORD).setDefault(DEFAULT_PASSWORD);
    hostGroup
        .addArgument("--" + Config.KEY_USE_ENCRYPTION)
        .help("Ues a TLS Bolt connection?")
        .setDefault(DEFAULT_USE_ENCRYPTION);

    ArgumentGroup udpGroup = parser.addArgumentGroup("UDP Event Listener");
    udpGroup
        .addArgument("--" + KEY_UDP_HOST)
        .help("IPv4 host to bind to")
        .setDefault(DEFAULT_UDP_HOST);
    udpGroup
        .addArgument("--" + KEY_UDP_PORT)
        .help("udp port to listen on")
        .setDefault(DEFAULT_UDP_PORT);
    udpGroup
        .addArgument("--" + KEY_BATCH_SIZE)
        .help("event buffer size")
        .setDefault(DEFAULT_BATCH_SIZE);
    udpGroup
        .addArgument("--" + KEY_WINDOW_TIMEOUT)
        .help("event buffer flush interval")
        .setDefault(DEFAULT_FLUSH_INTERVAL);
  }

  public String username;
  public String password;
  public String udpHost;
  public int udpPort;
  public String boltUri;
  public boolean useEncryption;
  public int bufferSize;
  public int flushInterval;

  public Config(String[] args) {
    Namespace ns;

    try {
      ns = parser.parseArgs(args);
    } catch (ArgumentParserException ape) {
      parser.handleError(ape);
      throw new ConfigException(ape.getLocalizedMessage(), Problem.ASKED_FOR_HELP);
    } catch (Exception e) {
      throw new ConfigException(e.getLocalizedMessage(), Problem.UNHANDLED_OPTION);
    }

    username = orString(ns, KEY_USERNAME, DEFAULT_USERNAME);
    password = orString(ns, KEY_PASSWORD, DEFAULT_PASSWORD);
    boltUri = orString(ns, KEY_BOLT_URI, DEFAULT_BOLT_URI);
    useEncryption = orBool(ns, KEY_USE_ENCRYPTION, DEFAULT_USE_ENCRYPTION);

    udpHost = orString(ns, KEY_UDP_HOST, DEFAULT_UDP_HOST);
    udpPort = orInt(ns, KEY_UDP_PORT, DEFAULT_UDP_PORT);
    if (0 > udpPort || udpPort > (0xffff - 2)) {
      throw new ConfigException("udp port out of valid range", Problem.INVALID_VALUE);
    }

    bufferSize = orInt(ns, KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE);
    if (250 > bufferSize || bufferSize > 10_000) {
      throw new ConfigException("buffer size out of a 'tolerable' range", Problem.INVALID_VALUE);
    }

    flushInterval = orInt(ns, KEY_WINDOW_TIMEOUT, DEFAULT_FLUSH_INTERVAL);
    if (0 > flushInterval || flushInterval > Integer.MAX_VALUE) {
      throw new ConfigException("flush interval must be positive", Problem.INVALID_VALUE);
    }
  }

  private static String convertToStupidArgParseKey(String key) {
    return key.replace("-", "_");
  }

  private static String orString(Namespace ns, String key, String defaultValue) {
    Object val = ns.get(convertToStupidArgParseKey(key));
    if (val == null) {
      return defaultValue;
    }
    return val.toString();
  }

  private static int orInt(Namespace ns, String key, int defaultValue) {
    Object val = ns.get(convertToStupidArgParseKey(key));
    try {
      return Integer.valueOf(val.toString()).intValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private static boolean orBool(Namespace ns, String key, boolean defaultValue) {
    Object val = ns.get(convertToStupidArgParseKey(key));
    try {
      return Boolean.valueOf(val.toString()).booleanValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  @Override
  public String toString() {
    return "Config{"
        + "username='"
        + username
        + '\''
        + ", password='********'"
        + ", udpHost='"
        + udpHost
        + '\''
        + ", udpPort="
        + udpPort
        + ", boltUri='"
        + boltUri
        + '\''
        + ", useEncryption="
        + useEncryption
        + ", bufferSize="
        + bufferSize
        + ", flushInterval="
        + flushInterval
        + '}';
  }
}
