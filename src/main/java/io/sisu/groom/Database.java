package io.sisu.groom;

import io.sisu.util.BulkQuery;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.summary.ResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
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
    public Integer writeSync(BulkQuery bulkQuery) {
        try (Session session = driver.session()) {
            session.writeTransaction(
                    tx -> {
                        ResultSummary result = tx.run(bulkQuery.query).consume();
                        logger.debug("wrote " + bulkQuery.size + " events to database");
                        return result;
                    });
            return bulkQuery.size;
        } catch (Exception e) {
            logger.error("writeSync(BQ) ERROR!: " + e.getMessage());
            throw e;
        }
    }

  public List<ResultSummary> writeSync(List<Query> queries) {
    List<ResultSummary> results = new ArrayList<>();

    try (Session session = driver.session()) {
      session.writeTransaction(
          tx -> {
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

  @Override
  public void close() throws Exception {
    driver.close();
  }
}
