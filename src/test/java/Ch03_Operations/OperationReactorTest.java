package Ch03_Operations;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

class OperationReactorTest {
    @Test
    public void Mapping() {
        Flux.range(2022, 5)
            .timestamp()
            .index()
            .map(data -> "index: " + data.getT1() +
                    ", ts: " + Instant.ofEpochMilli(data.getT2().getT1()) +
                    ", value: " + data.getT2().getT2())
            .subscribe(System.out::println);
    }

    @Test
    public void Filtering() throws InterruptedException {
        Mono<String> startCommand = Mono.just("start").delayElement(Duration.ofMillis(100));
        Mono<String> endCommand = Mono.just("end").delayElement(Duration.ofMillis(1000));

        Flux.interval(Duration.ofMillis(30))
            .filter(data -> data % 2 == 0)
            .skipUntilOther(startCommand) // 데이터가 발행될 때까지 스킵
            .takeUntilOther(endCommand) // 데이터가 발행될 때 끝
            .subscribe(System.out::println);

        Thread.sleep(1500);
    }

    @Test
    public void Collecting() {
        // 무한 스트림에서 collect는 메모리 부족으로 이어짐
        Flux.range(1, 5).collectSortedList(Comparator.reverseOrder()).subscribe(System.out::println);

        Flux.just(1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 1, 1, 1, 2, 2, 4, 5)
            .distinctUntilChanged()
            .subscribe(System.out::println);
    }

    @Test
    public void Scanning() {
        int bucketSize = 5;
        Flux.range(1, 500)
            .index()
            .scan(new int[bucketSize], (acc, elem) -> {
                acc[(int) (elem.getT1() % bucketSize)] = elem.getT2();
                return acc;
            })
            .skip(bucketSize)
            .map(arr -> Arrays.stream(arr).sum() * 1.0 / bucketSize)
            .subscribe(av -> System.out.println("Running average: " + av));
    }

    @Test
    public void ThenXXX() {
        Flux.just(1, 2, 3)
            .thenMany(Flux.just(4, 5)) // 상위 스트림 처리가 완료되고 즉시 새 스트림을 기동하는데 사용한다.
            .subscribe(e -> System.out.println("onNext: " + e));

        System.out.println("#################################");

        Flux.just(1, 2, 3).thenEmpty(Flux.empty()).subscribe(e -> System.out.println("onNext: " + e));

        System.out.println("#################################");

        Flux.just(1, 2, 3).then(Mono.just("Hello")).subscribe(e -> System.out.println("onNext: " + e));
    }

    @Test
    public void Concatenating() {
        /**
         * 1. concat // 첫 번째 모두 소비한 후 두 번째, 두 번째 모두 소비한 후 세 번째....
         * 2. merge // merge와 다르게 동시에 병합
         * 3. zip // 하나의 원소를 내보낼 때까지 모두 기다린 후 각각의 원소를 하나의 원소로 결합
         */
        Flux.concat(Flux.range(1, 3),
                    Flux.range(4, 2),
                    Flux.range(6, 5))
            .subscribe(e -> System.out.println("onNext: " + e));
    }

    @Test
    public void Batching() {
        /**
         * 1. Buffering => Flux<List<T>>
         * 2. Windowing => Flux<Flux<T>>
         * 3. Grouping => Flux<GroupedFlux<K, T>>
         */

        Flux.range(1, 13)
            .buffer(4)
            .subscribe(e -> System.out.println("onNext: " + e));

        Flux.range(101, 20)
            .windowUntil(data -> BigInteger.valueOf(data).isProbablePrime(100), true) // 해당 조건에 맞으면 앞에서 분할 아니면 뒤에서 분할 할 지 설정
            .subscribe(window -> window.collectList()
                                       .subscribe(e -> System.out.println("onNext: " + e)));

        Flux.range(1, 7)
            .groupBy(e -> e % 2 == 0 ? "Even" : "Odd")
            .subscribe(groupFlux -> groupFlux.scan(new LinkedList<>(), (list, elem) -> {
                                                 list.add(elem);
                                                 if (list.size() > 2) {
                                                     list.remove(0);
                                                 }
                                                 return list;
                                             })
                                             .filter(arr -> !arr.isEmpty())
                                             .subscribe(data -> System.out.println("key: " + groupFlux.key() + ", value: " + data)));
    }

    @Test
    public void FlatMapping() throws InterruptedException {
        Flux.just("user-1", "user-2", "user-3")
            .flatMap(u -> OperationReactor.requestBooks(u)
                                          .map(b -> u + "/" + b))
            .subscribe(data -> System.out.println("flatMap onNext: " + data));

        Thread.sleep(300);

        Flux.just("user-1", "user-2", "user-3")
            .concatMap(u -> OperationReactor.requestBooks(u)
                                            .map(b -> u + "/" + b))
            .subscribe(data -> System.out.println("concatMap onNext: " + data));

        Thread.sleep(300);

        Flux.just("user-1", "user-2", "user-3")
            .flatMapSequential(u -> OperationReactor.requestBooks(u)
                                                    .map(b -> u + "/" + b))
            .subscribe(data -> System.out.println("flatMapSequential onNext: " + data));

        // flatMapSequential과 concatMap의 차이는 concatMap은 하위 스트림 이전 스트림 내부 처리가 끝날 때 까지 기다린다.
        // 또한 concatMap은 원본과 동일한 순서를 유지하지만, flatMapSequential은 큐에 넣어 순서를 역순으로 유지한다.

        Thread.sleep(300);

        Flux.just("user-1", "user-2", "user-3")
            .switchMap(u -> OperationReactor.requestBooks(u)
                                            .map(b -> u + "/" + b))
            .subscribe(data -> System.out.println("switchMap onNext: " + data));

        Thread.sleep(300);
    }

    @Test
    public void Sampling() throws InterruptedException {
        Flux.range(1, 100)
            .delayElements(Duration.ofMillis(1))
            .sample(Duration.ofMillis(30))
            .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(550);
    }

}