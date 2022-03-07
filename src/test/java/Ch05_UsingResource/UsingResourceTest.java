package Ch05_UsingResource;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;

class UsingResourceTest {
    @Test
    public void Imperative() {
        try (UsingResource.Connection connection = UsingResource.newConnection()) {
            connection.getData().forEach(System.out::println);
        } catch (Exception e) {
            System.out.println("err");
        }
    }

    @Test
    public void Reactive() throws InterruptedException {
        final Flux<String> using = Flux.using(UsingResource::newConnection,
                                              con -> Flux.fromIterable(con.getData()),
                                              UsingResource.Connection::close);

        using.subscribe(data -> System.out.println("Receive data: " + data),
                        err -> System.out.println("Err: " + err),
                        () -> System.out.println("Complete!"));

        Thread.sleep(1000);
    }

    @Test
    public void UsingWhen() throws InterruptedException {
        Flux.usingWhen(UsingResource.Transaction.beginTransaction(),
                       transaction -> transaction.insertRows(Flux.just("A", "B", "C")),
                       UsingResource.Transaction::commit,
                       (transaction, err) -> transaction.rollback(),
                       UsingResource.Transaction::rollback)
            .subscribe(data -> System.out.println("onNext: " + data),
                       err -> System.out.println("onError: " + err),
                       () -> System.out.println("onComplete!"));

        Thread.sleep(1000);
    }

    @Test
    public void Error() throws InterruptedException {
        Flux.just("user-1")
            .flatMap(user -> UsingResource.recommendedBooks(user)
                                          .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
                                          .timeout(Duration.ofSeconds(3))
                                          .onErrorResume(e -> Flux.just("The Martian")))
            .subscribe(data -> System.out.println("onNext: " + data),
                       err -> System.out.println("onError: " + err),
                       () -> System.out.println("Complete!"));

        Thread.sleep(3000);
    }
}