package Ch02_Subscribe;


import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

public class SubscribeFluxMono {
    @Deprecated
    public static Subscriber<String> getSubscriber() {
        return new Subscriber<>() {
            volatile Subscription subscription;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                System.out.println("initial request for 1 elem");
                subscription.request(1);
            }

            @Override
            public void onNext(String item) {
                System.out.println("onNext: " + item);
                System.out.println("request for 1 more elem");
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("onError" + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                System.out.println("onComplete");
            }
        };
    }

    static class RecommendSubscriber<T> extends BaseSubscriber<T> {
        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            System.out.println("initial request for 1 elem");
            request(1);
        }

        @Override
        protected void hookOnNext(T value) {
            System.out.println("onNext: " + value);
            System.out.println("request for 1 more elem");
            request(1);
        }
    }

    public static RecommendSubscriber<String> getRecommendSubscriber() {
        return new RecommendSubscriber<>();
    }
}
