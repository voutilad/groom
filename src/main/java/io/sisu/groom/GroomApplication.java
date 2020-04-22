package io.sisu.groom;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.dropwizard.DropwizardConfig;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.sisu.groom.events.Event;
import io.sisu.util.BoundedArrayDeque;
import io.sisu.util.Pair;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

public class GroomApplication {
  private static final Duration WINDOW_DURATION = Duration.ofSeconds(2);
  private static final int WINDOW_SIZE = 2_500;

  // Set up nicer logging output.
  private static final Logger logger;

  static {
      System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
      System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "[yyyy-MM-dd'T'HH:mm:ss:SSS]");
      System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
      logger = LoggerFactory.getLogger(GroomApplication.class);

      prepareMetricSystem();
  }

  public static void reportPerformance(BoundedArrayDeque<Pair<Long, Integer>> queue) {
    // newest data is "in front", oldest "in back"
    final long timeDeltaMillis = (queue.getFirst().a - queue.getLast().a);
    final int eventSum = queue.getFirst().b - queue.getLast().b;
    if (timeDeltaMillis != 0) {
      logger.info(
          String.format(
              "current performance: sum[%d], rate[%d events/s] timeDelta[%d ms]",
              eventSum, Math.round(1000 * eventSum / timeDeltaMillis), timeDeltaMillis));
    }
  }

  public static void main(String[] args) throws Exception {
    logger.info("GROOM STARTING!");

    try (Database db = new Database(Database.defaultConfig, "neo4j", "secret")) {
      db.initializeSchema();

      final AtomicInteger eventCnt = new AtomicInteger(0);
      final AtomicInteger completedCnt = new AtomicInteger(0);

      // Our reporting stream...because it's fun to measure throughput. Sample every 5s, tracking
      // the last 6 samples at most so we get a moving average of the past ~30s or so.
      final BoundedArrayDeque<Pair<Long, Integer>> statsQueue = new BoundedArrayDeque<>(6);


      // Where the magic happens! Listen for a UDP stream of Doom Telemetry events and
      // batch insert them into the database.
      Connection conn =
          UdpServer.create()
              .host("0.0.0.0")
              .port(10666)
              .handle(
                  (in, out) ->
                      in.receive()
                          .asString()
                          .map(Event::fromJson) // Filter out invalid / unwanted elements
                          .name("incoming_events")
                          .metrics()
                          .onErrorContinue(Event.InvalidEventException.class, (e, o) -> logger.error("Crap event " + e.getMessage()))
                          .bufferTimeout(WINDOW_SIZE, WINDOW_DURATION) // Buffer a list of events
                          .concatMap( // Create a completely separate stream from here on, as the handler won't end due to UDPs nature
                              l -> Cypher.compileBulkEventComponentInsert(l)
                                  .flatMap(db::write)
                                  .then(db.write(Cypher.THREADING_QUERIES))
                                  .name("stored_bulks")
                                  .metrics()
                          )
                          .doOnComplete(() -> {
                              logger.info("Handler completed.");
                          })
              )
              .doOnBound(connection -> logger.info("READY FOR DATA!!! (ctrl-c to shutdown)"))
          .bindNow(Duration.ofSeconds(15));

        // Try to be kind and use a shutdown hook. This will hopefully let some data flush through.
        Runtime.getRuntime()
            .addShutdownHook(
                new Thread(
                    () -> {
                        logger.info("WAITING UP TO 15s FOR CONNECTION TO CLOSE");
                        conn.disposeNow(Duration.ofSeconds(15));
                    }));

        conn.onDispose().block();
        logger.info("GROOM SHUTTING DOWN!");
        logger.info("Received " + eventCnt.get() + " events, processed " + completedCnt.get());
    }
  }

    static void prepareMetricSystem() {
        DropwizardConfig consoleConfig = new DropwizardConfig() {

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
            new DropwizardMeterRegistry(consoleConfig, dropWizardMetricRegistry, HierarchicalNameMapper.DEFAULT,
                Clock.SYSTEM) {
                @Override
                protected Double nullGaugeValue() {
                    return null;
                }
            });
        ConsoleReporter reporter = ConsoleReporter.forRegistry(dropWizardMetricRegistry)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
        reporter.start(2, TimeUnit.SECONDS);
    }
}
