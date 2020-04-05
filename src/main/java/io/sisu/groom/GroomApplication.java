package io.sisu.groom;

import io.sisu.groom.events.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.Connection;
import reactor.netty.udp.UdpServer;

import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

public class GroomApplication {
	private static final Duration WINDOW_DURATION = Duration.ofSeconds(5);
	private static final int WINDOW_SIZE = 2_000;

	// Set up nicer logging output.
	private static final Logger logger;
	static {
		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "[yyyy-MM-dd'T'HH:mm:ss:SSS]");
		System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
		logger = LoggerFactory.getLogger(GroomApplication.class);
	}

	public static void main(String[] args) {
		AtomicInteger eventCnt = new AtomicInteger(0);
		logger.info("GROOM STARTING!");

		Connection conn = UdpServer.create()
				.host("0.0.0.0")
				.port(10666)
				.handle((in, out) ->
					in.receive().asString()
							.flatMap(Event::fromJson)
							.windowTimeout(WINDOW_SIZE, WINDOW_DURATION)
							.map(windowFlux ->
									windowFlux.collectSortedList(Comparator.comparing(Event::getFrame))
											.map(events -> {
												logger.info("In window, got " + events.size() + " events");
												logger.info("running total: " + eventCnt.addAndGet(events.size()));
												return events.size();
											})
											.subscribe())
							.then())
				.bindNow(Duration.ofSeconds(5));
		conn.onDispose().block();
		logger.info("GROOM DONE!");
	}
}
