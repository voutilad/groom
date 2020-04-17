package io.sisu.groom;

import io.sisu.groom.events.Event;
import org.neo4j.driver.Query;
import org.neo4j.driver.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class GroomApplication {
  private static final Duration WINDOW_DURATION = Duration.ofSeconds(5);
  private static final int WINDOW_SIZE = 500;

  // Set up nicer logging output.
  private static final Logger logger;

  static {
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "[yyyy-MM-dd'T'HH:mm:ss:SSS]");
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
    logger = LoggerFactory.getLogger(GroomApplication.class);
  }

  public static void main(String[] args) throws Exception {
    logger.info("GROOM STARTING!");

    try (Database db = new Database(Database.defaultConfig, "neo4j", "password")) {
      Arrays.stream(Cypher.SCHEMA_QUERIES)
          .forEach(
              query -> {
                try {
                  db.run(new Query(query)).block(Duration.ofSeconds(5));
                } catch (ClientException ce) {
                  if (ce.code().endsWith("EquivalentSchemaRuleAlreadyExists")) {
                    logger.info(
                        String.format(
                            "schema constraint already exists per cypher statement: %s", query));
                  } else {
                    throw ce;
                  }
                } catch (Exception e) {
                  logger.error(
                      String.format(
                          "Unexpected exception during db schema assertion: %s", e.getMessage()));
                  System.exit(1);
                }
              });

      final AtomicInteger cnt = new AtomicInteger(0);
      final long start = System.currentTimeMillis();

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
                              .map( events -> {
                                  int batchSize = events.size();
                                  cnt.addAndGet(batchSize);
                                  logger.info("processing batch with size: " + batchSize);
                                  return events;
                              })
                          .flatMap(Cypher::compileBulkEventComponentInsert)
                          .map(
                              query ->
                                  Arrays.asList(
                                      query,
                                      new Query(Cypher.THREAD_FRAMES),
                                      new Query(Cypher.THREAD_EVENTS),
                                      new Query(Cypher.THREAD_STATES),
                                      new Query(Cypher.CURRENT_STATE_DELETE),
                                      new Query(Cypher.CURRENT_STATE_UPDATE)))
                          .map(db::writeSync)
                          .onErrorMap(
                              throwable -> {
                                logger.error("OH CRAP !!!! " + throwable.getMessage());
                                return throwable;
                              })
                          .then())
              .doOnBound(connection -> logger.info("READY FOR DATA!!!"))
              .bindNow(Duration.ofSeconds(15));

      conn.onDispose().block();
      logger.info("GROOM SHUTTING DOWN!");
      logger.info("Finished after processing " + cnt.get() + " events");
    }
  }
}
