package io.sisu.groom;

import org.neo4j.driver.*;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.summary.ResultSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class Database implements AutoCloseable {
    private static Logger logger = LoggerFactory.getLogger(Database.class);

    public static final Config encryptedConfig =
            Config.builder()
                    .withLogging(Logging.slf4j())
                    .withEncryption().build();

    public static final Config defaultConfig =
            Config.builder()
                    .withLogging(Logging.slf4j()).build();

    private final Driver driver;

    Database(Config config, String username, String password) {
        driver = GraphDatabase.driver("neo4j://localhost:7687", AuthTokens.basic(username, password), config);
    }

    public Flux<Record> write(Query query) {
        return Flux.usingWhen(Mono.fromSupplier(driver::rxSession),
                session -> session.writeTransaction(tx -> Flux.from(tx.run(query).records())),
                RxSession::close);
    }

    public Flux<ResultSummary> writeBatch(List<Query> queries) {
        return Flux.usingWhen(Mono.fromSupplier(driver::rxSession),
                session -> session.writeTransaction(tx -> Flux.fromIterable(queries).map(tx::run).flatMap(RxResult::consume)),
                RxSession::close);
    }

    @Override
    public void close() throws Exception {
        driver.close();
    }
}
