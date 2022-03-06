package Ch01_FluxAndMono;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;

class FluxAndMonoTest {

    @Test
    public void FluxSequenceTest() {
        Flux<String> stream1 = Flux.just("Hello, ", "world");
        Flux<Integer> stream2 = Flux.fromArray(new Integer[]{1, 2, 3});
        Flux<Double> stream3 = Flux.fromIterable(Arrays.asList(10.0, 10.1, 10.2));
        Flux<Integer> stream4 = Flux.range(2010, 9);

        stream1.subscribe(System.out::println);
        stream2.subscribe(System.out::println);
        stream3.subscribe(System.out::println);
        stream4.subscribe(System.out::println);
    }

    @Test
    public void MonoTest() {
        Mono<String> stream5 = Mono.just("One");
        Mono<String> stream6 = Mono.justOrEmpty(null);
        Mono<String> stream7 = Mono.justOrEmpty(Optional.empty());
        Mono<String> stream8 = Mono.fromCallable(() -> "a");
        Mono<String> monoToFluxToMono = Mono.from(Flux.from(stream5));

        stream5.subscribe(System.out::println);
        stream6.subscribe(System.out::println);
        stream7.subscribe(System.out::println);
        stream8.subscribe(System.out::println);
        monoToFluxToMono.subscribe(System.out::println);
    }

    @Test
    public void FluxAndMonoEmptyTest() {
        // 빈 인스턴스
        Flux<String> empty = Flux.empty();
        Mono<String> monoEmpty = Mono.empty();
        // onNext, onComplete, onError에 대해서도 신호를 보내지 않음
        Flux<String> never = Flux.never();
        Mono<String> monoNever = Mono.never();
        // 항상 오류를 전파
        Flux<String> error = Flux.error(new RuntimeException());
        Mono<String> monoError = Mono.error(new RuntimeException());

        empty.subscribe(System.out::println);
        monoEmpty.subscribe(System.out::println);
        never.subscribe(System.out::println);
        monoNever.subscribe(System.out::println);
        error.subscribe(System.out::println);
        monoError.subscribe(System.out::println);
    }

    @Test
    public void DeferTest() {
        Random random = new Random();
        int id = random.nextInt(2);
        // defer는 구독하는 순간에 행동을 결정한다. 따라서 서로 다른 구독자에 대해 각각 다른 데이터를 생성할 수 있다.
        // id % 2 == 0는 실제로 구독이 발생할 때 까지 수행이 되지 않음
        Mono<Boolean> defer = Mono.defer(() -> id % 2 == 0 ? Mono.fromCallable(() -> Boolean.TRUE) : Mono.error(new RuntimeException()));
        defer.subscribe(System.out::println);
    }
}