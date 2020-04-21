package io.sisu.groom;

import io.sisu.groom.events.Event;
import io.sisu.util.BoundedArrayDeque;
import io.sisu.util.Pair;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

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

    try (Database db = new Database(Database.defaultConfig, "neo4j", "password")) {
      db.initializeSchema();

      final AtomicInteger eventCnt = new AtomicInteger(0);
      final AtomicInteger completedCnt = new AtomicInteger(0);

      // Our reporting stream...because it's fun to measure throughput. Sample every 5s, tracking
      // the last 6 samples at most so we get a moving average of the past ~30s or so.
      final BoundedArrayDeque<Pair<Long, Integer>> statsQueue = new BoundedArrayDeque<>(6);
      Flux.interval(Duration.ofSeconds(5))
          .map(
              unused -> {
                statsQueue.addFirst(new Pair(System.currentTimeMillis(), completedCnt.get()));
                reportPerformance(statsQueue);
                return Mono.empty();
              })
          .subscribe();

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
                          .flatMap(Event::fromJson)
                          .bufferTimeout(WINDOW_SIZE, WINDOW_DURATION)
                          .flatMap(Cypher::compileBulkEventComponentInsert)
                          .flatMap(db::write)
                          .map(completedCnt::addAndGet)
                          .onErrorMap(
                              throwable -> {
                                logger.error("OH CRAP !!!! " + throwable.getMessage());
                                return throwable;
                              })
                          .then())
              .doOnBound(connection -> logger.info("READY FOR DATA!!! (ctrl-c to shutdown)"))
              .bindNow(Duration.ofSeconds(15));

      // Handle setting up additional relationships
      Flux.interval(Duration.ofSeconds(5))
          .flatMap(i -> db.write(Cypher.THREADING_QUERIES))
          .subscribe();

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
}
