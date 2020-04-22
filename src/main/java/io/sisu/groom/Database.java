package io.sisu.groom;

import io.sisu.groom.exceptions.GroomDatabaseException;
import io.sisu.groom.exceptions.GroomDatabaseException.Problem;
import io.sisu.util.BulkQuery;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
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
  public static final Config defaultConfig = Config.builder().withLogging(Logging.slf4j()).build();
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
        driver = GraphDatabase.driver(boltUri, authToken, config);
        driver.verifyConnectivity();
      } catch (Exception e) {
        throw new GroomDatabaseException(e.getLocalizedMessage(), Problem.CONNECTION_FAILURE);
      }
      ensureAPOC(driver);
      initializeSchema(driver);
    }
    return driver;
  }

  public static void initializeSchema(Driver driver) throws GroomDatabaseException {
    try {
      Arrays.stream(Cypher.SCHEMA_QUERIES)
          .forEach(query -> {
            try (Session session = driver.session()) {
              session.run(query).consume();
            }
          });
    } catch (ClientException ce) {
      if (!ce.code().endsWith("EquivalentSchemaRuleAlreadyExists")) {
        throw new GroomDatabaseException(ce.getMessage(), Problem.SCHEMA_FAILURE);
      }
    }
  }

  private static void ensureAPOC(Driver driver) throws GroomDatabaseException {
    try (Session session = driver.session()) {
      session.run(Cypher.ENSURE_APOC).consume();
    } catch (ClientException ce) {
      throw new GroomDatabaseException(ce.getMessage(), Problem.MISSING_APOC);
    }
  }

  private static RxTransactionWork<Publisher<Integer>> writeEvents(BulkQuery bulkQuery) {
    return tx ->
        Mono.from(tx.run(bulkQuery.query).consume())
            .doOnSuccess(
                r -> logger.debug("Wrote {} events to database", r.counters().nodesCreated()))
            .doOnError(e -> logger.error("writeSync(BQ) ERROR!: " + e.getMessage()))
            .then(Mono.just(bulkQuery.size));
  }

  public Mono<Integer> write(BulkQuery bulkQuery) {
    return Flux.usingWhen(
            Mono.fromSupplier(connect()::rxSession),
            s -> s.writeTransaction(writeEvents(bulkQuery)),
            RxSession::close)
        .single();
  }

  public Mono<Void> write(List<Query> queries) {
    return Flux.usingWhen(
            Mono.fromSupplier(connect()::rxSession),
            s ->
                s.writeTransaction(
                    tx -> Flux.fromIterable(queries).map(tx::run).flatMap(RxResult::consume)),
            RxSession::close)
        .then();
  }

  @Override
  public void close() throws IOException {
    driver.close();
  }
}
