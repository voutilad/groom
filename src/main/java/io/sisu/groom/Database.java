package io.sisu.groom;

import org.neo4j.driver.*;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.summary.ResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

  public Flux<ResultSummary> write(Query query) {
    return Flux.usingWhen(
        Mono.fromSupplier(driver::rxSession),
        session ->
            session.writeTransaction(
                tx ->
                    Flux.just(query)
                        .map(tx::run)
                        .flatMap(RxResult::consume)
                        .onErrorMap(
                            t -> {
                              logger.error("WHAT THE HECK: " + t.getMessage());
                              return t;
                            }),
                txConfig),
        RxSession::close);
  }

  public List<ResultSummary> writeSync(List<Query> queries) {
      List<ResultSummary> results = new ArrayList<>();

      try (Session session = driver.session()) {
          session.writeTransaction(tx -> {
            for (Query q : queries) {
                results.add(tx.run(q).consume());
            }
            return results.size();
          });
          return results;
      } catch (Exception e) {
          logger.error("writeSync ERROR!: " + e.getMessage());
          return results;
      }
  }

  public Mono<Record> run(Query query) {
    return Mono.usingWhen(
        Mono.fromSupplier(driver::rxSession),
        session -> Mono.from(session.run(query).records()),
        RxSession::close);
  }

  public Flux<ResultSummary> writeBatch(List<Query> queries) {
    return Flux.usingWhen(
        Mono.fromSupplier(driver::rxSession),
        session ->
            session.writeTransaction(
                tx ->
                    Flux.fromIterable(queries)
                        .map(tx::run)
                        .flatMap(RxResult::consume)
                        .retry(10)
                        .onErrorMap(
                            throwable -> {
                              logger.error("OH CRAP OH CRAP OH NO! " + throwable.getMessage());
                              return throwable;
                            })),
        RxSession::close);
  }

  @Override
  public void close() throws Exception {
    driver.close();
  }
}
