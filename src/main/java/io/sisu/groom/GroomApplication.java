package io.sisu.groom;

import io.sisu.groom.events.Event;
import io.sisu.util.BoundedArrayDeque;
import io.sisu.util.Pair;
import org.neo4j.driver.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GroomApplication {
  private static final Duration WINDOW_DURATION = Duration.ofSeconds(2);
  private static final int WINDOW_SIZE = 1_000;

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
    logger.info(
        String.format(
            "current performance: sum[%d], rate[%d events/s] timeDelta[%d ms]", eventSum,
            Math.round(1000 * eventSum / timeDeltaMillis), timeDeltaMillis));
  }

  public static void main(String[] args) throws Exception {
    logger.info("GROOM STARTING!");

    try (Database db = new Database(Database.defaultConfig, "neo4j", "password")) {
      db.initializeSchema();

      final AtomicInteger eventCnt = new AtomicInteger(0);
      final AtomicInteger completedCnt = new AtomicInteger(0);
      final BoundedArrayDeque<Pair<Long, Integer>> statsQueue = new BoundedArrayDeque<>(50);
      statsQueue.push(new Pair(System.currentTimeMillis(), 0));

      Connection conn =
          UdpServer.create()
              .host("127.0.0.1")
              .port(10666)
              .handle(
                  (in, out) ->
                      in.receive()
                          .asString()
                          .flatMap(Event::fromJson)
                          .bufferTimeout(WINDOW_SIZE, WINDOW_DURATION)
                          .flatMap(Cypher::compileBulkEventComponentInsert)
                          .map(db::writeSync)
                          .flatMap(
                              total -> {
                                statsQueue.addFirst(
                                    new Pair(System.currentTimeMillis(), completedCnt.addAndGet(total)));
                                reportPerformance(statsQueue);
                                return Mono.empty();
                              })
                          .onErrorMap(
                              throwable -> {
                                logger.error("OH CRAP !!!! " + throwable.getMessage());
                                return throwable;
                              })
                          .then())
              .doOnBound(connection -> logger.info("READY FOR DATA!!! (ctrl-c to shutdown)"))
              .bindNow(Duration.ofSeconds(15));

      final List<Query> threadingQueries =
          Arrays.asList(
              new Query(Cypher.THREAD_FRAMES),
              new Query(Cypher.THREAD_EVENTS),
              new Query(Cypher.THREAD_STATES),
              new Query(Cypher.CURRENT_STATE_DELETE),
              new Query(Cypher.CURRENT_STATE_UPDATE));

      Flux.interval(Duration.ofSeconds(5))
          .map(
              junk -> {
                logger.debug("starting threading...");
                db.writeSync(threadingQueries);
                logger.debug("done threading.");
                return 0;
              })
          .subscribe();

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
