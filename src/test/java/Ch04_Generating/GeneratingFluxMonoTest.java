package Ch04_Generating;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Signal;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class GeneratingFluxMonoTest {
    @Test
    public void Blocking() {
        Flux<String> hello1 = Flux.just("Hello", "World!");
        Iterable<String> strings = hello1.toIterable();
        strings.forEach(System.out::println);

        Flux<String> hello2 = Flux.just("Hello", "World!");
        Stream<String> stringStream = hello2.toStream();
        stringStream.forEach(System.out::println);
    }

    @Test
    public void doOnXXX() {
        Flux.just(1, 2, 3)
            .concatWith(Flux.error(new RuntimeException("Conn error")))
            .doOnSubscribe(subscription -> System.out.println("doOnSubscription: " + subscription))
            .doOnNext(data -> System.out.println("doOnNext: " + data))
            .doOnError(err -> System.out.println("doOnError: " + err))
            .doOnTerminate(() -> System.out.println("doOnTerminated"))
            .doOnEach(s -> System.out.println("signal: " + s))
            .subscribe();
    }

    @Test
    public void Materializing() {
        Flux.range(1, 3)
            .doOnNext(data -> System.out.println("doOnNext: " + data))
            .materialize()
            .doOnNext(data -> System.out.println("doOnNextMaterialize(signal): " + data))
            .dematerialize()
            .doOnNext(data -> System.out.println("doOnNextDematerialize: " + data))
            .collectList()
            .subscribe(System.out::println);
    }

    @Test
    // 비동기, 다중 값, 단일 스레드
    public void Push() throws InterruptedException {
        Flux<Object> objectFlux = Flux.push(emitter -> //  push 메서드 내에서는 배압과 취소에 신경쓰지 않아도 이런 기능을 지원한다.
                                            {
                                                IntStream.range(2000, 3000) // 기존 API 2000 ~ 3000까지의 숫자 생성
                                                         .forEach(emitter::next);
                                                emitter.complete();
                                            }) // FluxSink 타입으로 전송
                                      .delayElements(Duration.ofMillis(1));
        objectFlux.subscribe(data -> System.out.println("onNext: " + data));
        Thread.sleep(1500);
        objectFlux.count().subscribe(data -> System.out.println("expected: 1000, actual: " + data));
        Thread.sleep(1500);
    }

    @Test
    public void pushMultiThread() throws InterruptedException {
        Flux<Signal<Object>> materialize = Flux.push(emitter -> {
                                                   IntStream.range(2000, 3000)
                                                            .parallel()
                                                            .forEach(emitter::next);
                                                   emitter.complete();
                                               })
                                               .materialize();
        materialize.count().subscribe(data -> System.out.println("expected: 1000, actual: " + data));
        Thread.sleep(1000);
    }

    @Test
    // 비동기, 다중 값, 다중 스레드
    public void Create() throws InterruptedException {
        Flux<Object> objectFlux = Flux.create(emitter -> {
            IntStream.range(2000, 3000)
                     .parallel()
                     .forEach(emitter::next);
            emitter.complete();
        });
        objectFlux.subscribe(data -> System.out.println("onNext: " + data));
        Thread.sleep(1500);
        objectFlux.count().subscribe(data -> System.out.println("expected: 1000, actual: " + data));
        Thread.sleep(1500);
    }

    @Test
    // 동기
    public void Generate() throws InterruptedException {
        Flux.generate(() -> Tuples.of(0, 1), // 이전 상태값을 기반으로 다음 내부 상태를 계산하고 onNext 신호 전달
                      (state, sink) -> {
                          System.out.println("generated value: " + state.getT2());
                          sink.next(state.getT2());
                          int newValue = state.getT1() + state.getT2();
                          return Tuples.of(state.getT2(), newValue);
                      })
            .take(7L)
            .delayElements(Duration.ofMillis(10))
            .subscribe(data -> System.out.println("onNext: " + data));
        Thread.sleep(500);
    }

}