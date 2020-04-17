package io.sisu.groom.events;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

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

    a.map(o -> { System.out.println("o: " + o); return o;}).publishOn(Schedulers.immediate(), 1).concatWith(b).subscribe(i -> {
      System.out.println(i);
      //return 0;
    });
  }
}
