package io.sisu.groom;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.sisu.groom.events.Event;
import io.sisu.groom.exceptions.InvalidEventException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

public class GroomApplication {
  private static final Logger logger;

  private static final String banner =
      "\n"
          + "          _____ ______  _____  _____ ___  ___\n"
          + "         |  __ \\| ___ \\|  _  ||  _  ||  \\/  |\n"
          + "         | |  \\/| |_/ /| | | || | | || .  . |\n"
          + "         | | __ |    / | | | || | | || |\\/| |\n"
          + "         | |_\\ \\| |\\ \\ \\ \\_/ /\\ \\_/ /| |  | |\n"
          + "          \\____/\\_| \\_| \\___/  \\___/ \\_|  |_/";

  static {
    // Set up nicer logging output.
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "[yyyy-MM-dd'T'HH:mm:ss:SSS]");
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
    logger = LoggerFactory.getLogger(GroomApplication.class);

    prepareMetricSystem();
  }

  public static void main(String[] args) throws Exception {
    Config config = new Config(args);

    logger.info(banner);
    logger.info("Using configuration: {}", config);
    GroomApplication app = new GroomApplication();
    app.run(config);
    Metrics.globalRegistry.close();
    logger.info("GROOM SHUTTING DOWN!");
  }

  public void run(Config config) throws Exception {
    final Duration windowDuration = Duration.ofSeconds(config.flushInterval);

    try (Database db = new Database(config, config.username, config.password)) {
      // Make sure we can connect
      db.connect();

      // Where the magic happens! Listen for a UDP stream of Doom Telemetry events and
      // batch insert them into the database.
      Connection conn =
          UdpServer.create()
              .host(config.udpHost)
              .port(config.udpPort)
              .handle(
                  (in, out) ->
                      in.receive()
                          .asString()
                          .name("incoming_strings")
                          .metrics()
                          .map(Event::fromJson) // Filter out invalid / unwanted elements
                          .name("incoming_events")
                          .metrics()
                          .onErrorContinue(
                              InvalidEventException.class,
                              (e, o) -> logger.error("Crap event: " + e.getMessage()))
                          .bufferTimeout(
                              config.bufferSize, windowDuration) // Buffer a list of events
                          .concatMap(
                              // Create a completely separate stream from here on, as the
                              // handler won't end due to UDPs nature
                              l ->
                                  Cypher.compileBulkEventComponentInsert(l)
                                      .flatMap(db::write)
                                      .then(db.write(Cypher.THREADING_QUERIES))
                                      .name("stored_bulks")
                                      .metrics())
                          .doOnComplete(() -> logger.info("udp handler completed")))
              .doOnBound(
                  connection ->
                      logger.info(
                          "now listening on {}:{} (send ctrl-c to shutdown)",
                          config.udpHost,
                          config.udpPort))
              .bindNow(Duration.ofSeconds(15));

      // Try to be kind and use a shutdown hook. This will hopefully let some data flush through.
      Runtime.getRuntime()
          .addShutdownHook(new Thread(() -> conn.disposeNow(Duration.ofSeconds(15))));

      conn.onDispose().block();
    }
  }

  static void prepareMetricSystem() {
    DropwizardConfig consoleConfig =
        new DropwizardConfig() {

          @Override
          public String prefix() {
            return "console";
          }

          @Override
          public String get(String key) {
            return null;
          }
        };

    MetricRegistry dropWizardMetricRegistry = new MetricRegistry();
    Metrics.globalRegistry.add(
        new DropwizardMeterRegistry(
            consoleConfig, dropWizardMetricRegistry, HierarchicalNameMapper.DEFAULT, Clock.SYSTEM) {
          @Override
          protected Double nullGaugeValue() {
            return null;
          }
        });

    Slf4jReporter.forRegistry(dropWizardMetricRegistry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build()
        .start(10, TimeUnit.SECONDS);
  }
}
