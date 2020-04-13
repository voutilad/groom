package io.sisu.groom;

import io.sisu.groom.events.Event;
import org.neo4j.driver.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GroomApplication {
	private static final Duration WINDOW_DURATION = Duration.ofSeconds(5);
	private static final int WINDOW_SIZE = 200;

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
			Arrays.stream(Cypher.SCHEMA_QUERIES).forEach(query -> {
				try {
					db.write(new Query(query)).blockLast(Duration.ofSeconds(5));
				} catch (Exception e) {
					System.err.println("Could not execute " + query);
				}
			});

			Connection conn = UdpServer.create()
					.host("0.0.0.0")
					.port(10666)
					.handle((in, out) ->
							in.receive().asString()
									//.log("RAWJSON")
									.flatMap(Event::fromJson)
									.windowTimeout(WINDOW_SIZE, WINDOW_DURATION)
									.map(windowFlux ->
											windowFlux.collectSortedList(Comparator.comparing(Event::getFrame))
													.map(Cypher::compileBulkEventComponentInsert)
													.map(query -> db.write(query).subscribe())
													.subscribe())
									.then())
					.doOnBound(connection -> logger.info("READY!!!"))
					.bindNow(Duration.ofSeconds(5));
			conn.onDispose().block();
		}
		logger.info("GROOM DONE!");
	}
}
