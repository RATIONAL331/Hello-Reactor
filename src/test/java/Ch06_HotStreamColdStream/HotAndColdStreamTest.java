package Ch06_HotStreamColdStream;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

class HotAndColdStreamTest {
    @Test
    public void ColdPublisher() {
        final Flux<String> coldPublisher = Flux.defer(() -> {
            System.out.println("Generating new items");
            return Flux.just(UUID.randomUUID().toString());
        });

        System.out.println("No data was generated so far");
        coldPublisher.subscribe(data -> System.out.println("onNext#1: " + data));
        coldPublisher.subscribe(data -> System.out.println("onNext#2: " + data));
        System.out.println("Data was generated twice for two subscribers");
    }

    @Test
    public void ConnectableFluxPublisher() {
        final Flux<Integer> integerFlux = Flux.range(0, 3)
                                              .doOnSubscribe(subscription -> System.out.println("new Subscription for the cold publisher"));

        final ConnectableFlux<Integer> publish = integerFlux.publish();
        publish.subscribe(data -> System.out.println("onNext#1: " + data));
        publish.subscribe(data -> System.out.println("onNext#2: " + data));
        System.out.println("All subscribers are ready!");

        publish.connect();
        // integerFlux는 한 번만 데이터를 생성하였고 구독자들은 그 데이터들을 모두 수신함.
    }

    @Test
    public void FluxCaching() throws InterruptedException {
        final Flux<Integer> integerFlux = Flux.range(0, 2)
                                              .doOnSubscribe(subscription -> System.out.println("new Subscription for the cold publisher"));

        final Flux<Integer> cache = integerFlux.cache(Duration.ofSeconds(1)); // 1초 동안 Cold Publisher를 캐싱

        cache.subscribe(data -> System.out.println("onNext#1: " + data));
        cache.subscribe(data -> System.out.println("onNext#2: " + data)); // 첫 번째 구독자와 동일한 데이터를 공유

        Thread.sleep(1200); // 캐싱이 만료될 때 까지 대기

        cache.subscribe(data -> System.out.println("onNext#3: " + data)); // 캐싱된 데이터를 찾을 수 없기 때문에 Cold Publisher는 다시 데이터 생성
    }

    @Test
    public void FluxSharing() throws InterruptedException {
        final Flux<Integer> integerFlux = Flux.range(0, 5)
                                              .delayElements(Duration.ofMillis(100))
                                              .doOnSubscribe(subscription -> System.out.println("new Subscription for the cold publisher"));

        final Flux<Integer> share = integerFlux.share();

        share.subscribe(data -> System.out.println("onNext#1: " + data));
        share.subscribe(data -> System.out.println("onNext#2: " + data));

        Thread.sleep(400);

        share.subscribe(data -> System.out.println("onNext#3: " + data)); // 3과 4만 수신

        Thread.sleep(400);
    }

    @Test
    public void Elapsed() throws InterruptedException {
        Flux.range(0, 5)
            .delayElements(Duration.ofMillis(100))
            .elapsed()
            .subscribe(data -> System.out.println("Elapsed: " + data.getT1() + " ms: " + data.getT2()));

        Thread.sleep(1000);
    }

    @Test
    public void Transforming() {
        Flux.range(1000, 3)
            .map(integer -> "user-" + integer)
            .transform(stringFlux -> stringFlux.index()
                                               .doOnNext(objects -> System.out.println("index: " + objects.getT1() + " user: " + objects.getT2()))
                                               .map(Tuple2::getT2))
            .subscribe(data -> System.out.println("onNext: " + data));
        // transform 연산자는 결합 단계에서 스트림 동작을 한 번만 변경한다.
    }

    @Test
    public void Compose() throws InterruptedException {
        final Flux<String> transformDeferred = Flux.just("1", "2")
                                                   .transformDeferred(stringFlux -> {
                                                       if (new Random().nextBoolean()) {
                                                           return stringFlux.doOnNext(data -> System.out.println("TRUE: " + data));
                                                       } else {
                                                           return stringFlux.doOnNext(data -> System.out.println("FALSE: " + data));
                                                       }
                                                   });
        transformDeferred.subscribe();
        transformDeferred.subscribe();

        Thread.sleep(500);
        // transform 과 다른 점은 구독자가 도착할 때 마다 변환작업을 수행한다.
    }
}