package io.sisu.groom;

import io.sisu.groom.events.Event;
import org.neo4j.driver.Query;
import org.neo4j.driver.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;

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
                  db.write(new Query(query)).blockLast(Duration.ofSeconds(5));
                } catch (ClientException ce) {
                  if (ce.code().endsWith("EquivalentSchemaRuleAlreadyExists")) {
                    logger.info("schema constraint already exists per cypher statement: %s", query);
                  } else {
                    throw ce;
                  }
                } catch (Exception e) {
                  logger.error("Unexpected exception during db schema assertion: %s", e.getMessage());
                  System.exit(1);
                }
              });

      Connection conn =
          UdpServer.create()
              .host("127.0.0.1")
              .port(10666)
              .handle(
                  (in, out) ->
                      in.receive()
                          .asString()
                          .flatMap(Event::fromJson)
                          .windowTimeout(WINDOW_SIZE, WINDOW_DURATION)
                          .map(
                              windowFlux ->
                                  windowFlux
                                      .collectSortedList(Comparator.comparing(Event::getFrame))
                                      .filter(list -> !list.isEmpty())
                                      .map(
                                          eventList -> {
                                            logger.info("processing event list with size %d", eventList.size());
                                            return Cypher.compileBulkEventComponentInsert(
                                                eventList);
                                          })
                                      .map(query -> db.write(query).subscribe())
                                      .subscribe())
                          .then())
              .doOnBound(connection -> logger.info("READY FOR DATA!!!"))
              .bindNow(Duration.ofSeconds(15));

      System.in.read();
      logger.info("GROOM SHUTTING DOWN!");
      conn.disposeNow();
    }
  }
}
