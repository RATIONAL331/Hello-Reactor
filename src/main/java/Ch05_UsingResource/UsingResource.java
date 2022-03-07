package Ch05_UsingResource;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Random;

public class UsingResource {
    private static final Random rnd = new Random();

    public static class Connection implements AutoCloseable {
        private static final Random rnd = new Random();

        public Iterable<String> getData() {
            if (rnd.nextInt(2) == 1) {
                throw new RuntimeException("Communication Error");
            }
            return Arrays.asList("Some", "data");
        }

        @Override
        public void close() {
            System.out.println("Connection closed.");
        }
    }

    public static Connection newConnection() {
        System.out.println("Connection created.");
        return new Connection();
    }

    public static class Transaction {
        private static final Random rnd = new Random();
        private final int id;

        public Transaction(int id) {
            this.id = id;
        }

        public static Mono<Transaction> beginTransaction() {
            return Mono.defer(() -> Mono.just(new Transaction(rnd.nextInt(1000))));
        }

        public Flux<String> insertRows(Publisher<String> rows) {
            return Flux.from(rows)
                       .delayElements(Duration.ofMillis(100))
                       .flatMap(row -> {
                           if (rnd.nextInt(3) == 1) {
                               return Mono.error(new RuntimeException("Error: " + row));
                           } else {
                               return Mono.just(row);
                           }
                       });
        }

        public Mono<Void> commit() {
            return Mono.defer(() -> {
                System.out.println("Transaction " + id + " commit");
                if (rnd.nextBoolean()) {
                    return Mono.empty();
                } else {
                    return Mono.error(new RuntimeException("Conflict"));
                }
            });
        }

        public Mono<Void> rollback() {
            return Mono.defer(() -> {
                System.out.println("Transaction " + id + " rollback");
                if (rnd.nextBoolean()) {
                    return Mono.empty();
                } else {
                    return Mono.error(new RuntimeException("Conflict"));
                }
            });
        }
    }

    public static Flux<String> recommendedBooks(String userId) {
        return Flux.defer(() -> {
            if (rnd.nextInt(11) > 1) {
                return Flux.<String>error(new RuntimeException("Err")).delaySequence(Duration.ofMillis(100));
            } else {
                return Flux.just("Blue Mars", "The Expanse").delayElements(Duration.ofMillis(50));
            }
        }).doOnSubscribe(data -> System.out.println("Request for " + userId));
    }
}
