package Ch03_Operations;

import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Random;

public class OperationReactor {
    private static final Random random = new Random();
    public static Flux<String> requestBooks(String user) {
        return Flux.range(1, random.nextInt(3 ) + 1)
                   .map(i -> "book-" + i)
                   .delayElements(Duration.ofMillis(3));
    }
}
