package io.sisu.groom.events;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

public class FluxFun {
  static void randomlyFail() throws Exception {
    if (System.nanoTime() % 2 == 0) {
      throw new Exception("Crap");
    }
  }

  @Test
  void junk() throws Exception {

    Flux a = Flux.range(1, 4);
    Flux b = Flux.fromArray(new String[] {"dave", "dan", "jess"});

    a.flatMap(val -> Flux.just(val).concatWith(b)).subscribe(System.out::println);
  }
}
