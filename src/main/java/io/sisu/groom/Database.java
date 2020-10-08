package io.sisu.groom;

import io.sisu.groom.exceptions.GroomDatabaseException;
import io.sisu.groom.exceptions.GroomDatabaseException.Problem;
import io.sisu.util.BulkQuery;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransactionWork;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Database implements Closeable {
  public static final org.neo4j.driver.Config defaultConfig = org.neo4j.driver.Config.builder().withLogging(Logging.slf4j()).build();
  private static Logger logger = LoggerFactory.getLogger(Database.class);

  private Driver driver;
  private String boltUri = "bolt://localhost:7687";
  private Config config;
  private AuthToken authToken;

  Database(Config config, String username, String password) {
    this.config = config;
    this.authToken = AuthTokens.basic(username, password);
  }

  public Driver connect() throws GroomDatabaseException {
    if (driver == null) {
      try {
        driver = GraphDatabase.driver(config.boltUri, authToken, defaultConfig);
        driver.verifyConnectivity();
      } catch (Exception e) {
        throw new GroomDatabaseException(e.getLocalizedMessage(), Problem.CONNECTION_FAILURE);
      }
      ensureAPOC(driver, config);
      initializeSchema(driver, config);
    }
    return driver;
  }

  public static void initializeSchema(Driver driver, Config config) throws GroomDatabaseException {
    try (Session session = driver.session(SessionConfig.forDatabase(config.dbName))) {
      session.run(Cypher.SCHEMA_ASSERT).consume();
    } catch (ClientException ce) {
      if (!ce.code().endsWith("EquivalentSchemaRuleAlreadyExists")) {
        throw new GroomDatabaseException(ce.getMessage(), Problem.SCHEMA_FAILURE);
      }
    }
  }

  private static void ensureAPOC(Driver driver, Config config) throws GroomDatabaseException {
    try (Session session = driver.session(SessionConfig.forDatabase(config.dbName))) {
      session.run(Cypher.ENSURE_APOC).consume();
    } catch (ClientException ce) {
      throw new GroomDatabaseException(ce.getMessage(), Problem.MISSING_APOC);
    }
  }

  private static RxTransactionWork<Publisher<Integer>> writeEvents(BulkQuery bulkQuery) {
    return tx ->
        Mono.from(tx.run(bulkQuery.query).consume())
            .doOnSuccess(
                r ->
                    logger.info(
                        "wrote {} events to database, creating {} nodes",
                        bulkQuery.size,
                        r.counters().nodesCreated()))
            .doOnError(e -> logger.error("writeEvents ERROR!: {}", e.getLocalizedMessage()))
            .then(Mono.just(bulkQuery.size));
  }

  public Mono<Integer> write(BulkQuery bulkQuery) {
    return Flux.usingWhen(
            Mono.fromSupplier(() -> connect().rxSession(SessionConfig.forDatabase(config.dbName))),
            s -> s.writeTransaction(writeEvents(bulkQuery)),
            RxSession::close)
        .single();
  }

  public Mono<Void> write(List<Query> queries) {
    return Flux.usingWhen(
            Mono.fromSupplier(() -> connect().rxSession(SessionConfig.forDatabase(config.dbName))),
            s ->
                s.writeTransaction(
                    tx ->
                        Flux.fromIterable(queries)
                            .map(tx::run)
                            .flatMap(RxResult::consume)
                            .doOnComplete(() -> logger.info("write(L<Q>) complete, executed {} queries", queries.size()))
                            .doOnError(
                                e ->
                                    logger.error(
                                        "write(L<Q>) ERROR!: {}", e.getLocalizedMessage()))),
            RxSession::close)
        .then();
  }

  @Override
  public void close() throws IOException {
    driver.close();
  }
}
