package io.sisu.groom;

import io.sisu.groom.events.Event;
import org.neo4j.driver.Query;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.summary.ResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GroomApplication {
  private static final Duration WINDOW_DURATION = Duration.ofSeconds(5);
  private static final int WINDOW_SIZE = 5000;

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

      final AtomicInteger eventCnt = new AtomicInteger(0);
      final AtomicInteger completedCnt = new AtomicInteger(0);

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
                          .map(
                              events -> {
                                int batchSize = events.size();
                                eventCnt.addAndGet(batchSize);
                                logger.info("processing batch with size: " + batchSize);
                                return events;
                              })
                          .flatMap(Cypher::compileBulkEventComponentInsert)
                          .map(
                              bulkQuery -> {
                                long start = System.currentTimeMillis();
                                List<ResultSummary> results =
                                    db.writeSync(
                                        Arrays.asList(
                                            bulkQuery.query,
                                            new Query(Cypher.THREAD_FRAMES),
                                            new Query(Cypher.THREAD_EVENTS),
                                            new Query(Cypher.THREAD_STATES),
                                            new Query(Cypher.CURRENT_STATE_DELETE),
                                            new Query(Cypher.CURRENT_STATE_UPDATE)));
                                completedCnt.addAndGet(bulkQuery.size);
                                logger.info(
                                    String.format(
                                        "current db insertion rate: %d/s",
                                        Math.round(
                                            1000f
                                                * bulkQuery.size
                                                / (System.currentTimeMillis() - start))));
                                return results;
                              })
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
      logger.info("Received " + eventCnt.get() + " events, processed " + completedCnt.get());
    }
  }
}
