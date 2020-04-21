package io.sisu.groom;

import io.sisu.util.BulkQuery;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransactionWork;
import org.neo4j.driver.summary.ResultSummary;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Database implements AutoCloseable {
  public static final Config encryptedConfig =
      Config.builder().withLogging(Logging.slf4j()).withEncryption().build();
  public static final Config defaultConfig = Config.builder().withLogging(Logging.slf4j()).build();
  private static Logger logger = LoggerFactory.getLogger(Database.class);
  private final Driver driver;
  private final TransactionConfig txConfig =
      TransactionConfig.builder().withTimeout(Duration.ofSeconds(30)).build();

  Database(Config config, String username, String password) {
    driver =
        GraphDatabase.driver(
            "neo4j://localhost:7687", AuthTokens.basic(username, password), config);
  }

  public void initializeSchema() {
    Arrays.stream(Cypher.SCHEMA_QUERIES)
        .forEach(
            query -> {
              try {
                run(new Query(query)).block(Duration.ofSeconds(5));
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
  }

  public Mono<Integer> write(BulkQuery bulkQuery) {
    return Flux.usingWhen(
            Mono.fromSupplier(driver::rxSession),
            s -> s.writeTransaction(writeEvents(bulkQuery)),
            RxSession::close)
        .single();
  }

  private static RxTransactionWork<Publisher<Integer>> writeEvents(BulkQuery bulkQuery) {
    return tx ->
        Mono.from(tx.run(bulkQuery.query).consume())
            .doOnSuccess(
                r -> logger.debug("Wrote {} events to database", r.counters().nodesCreated()))
            .doOnError(e -> logger.error("writeSync(BQ) ERROR!: " + e.getMessage()))
            .then(Mono.just(bulkQuery.size));
  }

  public Mono<Void> write(List<Query> queries) {
    return Flux.usingWhen(
            Mono.fromSupplier(driver::rxSession),
            s ->
                s.writeTransaction(
                    tx -> Flux.fromIterable(queries).map(tx::run).flatMap(RxResult::consume)),
            RxSession::close)
        .then();
  }

  public Mono<Record> run(Query query) {
    return Mono.usingWhen(
        Mono.fromSupplier(driver::rxSession),
        session -> Mono.from(session.run(query).records()),
        RxSession::close);
  }

  @Override
  public void close() throws Exception {
    driver.close();
  }
}
