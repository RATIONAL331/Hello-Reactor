package Ch02_Subscribe;

import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;

class SubscribeFluxMonoTest {

    @Test
    public void FluxBasicSubscribe() {
        Flux.just("A", "B", "C")
            .subscribe(data -> System.out.println("onNext: " + data),
                       err -> { /* err ignore */ },
                       () -> System.out.println("onComplete"));
    }

    @Test
    public void FluxSubscribeWithSubscription() {
        Flux.range(1, 100)
            .subscribe(data -> System.out.println("onNext: " + data), // 직접 제어는 비권장
                       err -> { /* err ignored */ },
                       () -> System.out.println("onComplete"),
                       subscription -> {
                           subscription.request(4);
                           subscription.cancel(); // 도중에 끝냈기 때문에 onComplete는 호출되지 않음
                       });
    }

    @Test
    public void FluxReturnDisposable() throws InterruptedException {
        Disposable disposable = Flux.interval(Duration.ofMillis(50))
                                    .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(200);
        disposable.dispose(); // 외부에서 cancel() 호출하는 역할
    }

    @Test
    public void FluxSubscribeWithExplicitSubscription() {
        Flux<String> stream = Flux.just("Hello", "World", "!");
        stream.subscribe(SubscribeFluxMono.getSubscriber()); // Subscriber를 직접 구현하는 일은 비권장
        // 스스로 배압을 관리하고, 구독 확인, 취소와 관련된 TCK 요구사항 위반
    }

    @Test
    public void FluxSubscribeWithExplicitSubscription2() {
        Flux<String> stream = Flux.just("Hello", "World", "!");
        stream.subscribe(SubscribeFluxMono.getRecommendSubscriber());
    }
}