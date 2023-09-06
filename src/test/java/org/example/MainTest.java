package org.example;

import org.example.model.Request;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.String.format;

class MainTest {

    private Main cut;
    private Clock clock;

    @BeforeEach
    void setUp() {
        this.clock = Clock.systemDefaultZone();
        this.cut = new Main(clock);
    }

    @Test
    void testSingleRequest() throws InterruptedException {
        var request = new Request("1.1.1.1", "Agent X");

        cut.processRequest(request);
    }

    @Test
    void testMultipleRequestsWithDifferentIpAgentsPairs() {
        var requestList = generateRequestsWithDifferentIpAgent();

        Assertions.assertTrue(requestList.stream().allMatch(cut::processRequest));
    }

    @Test
    void given5SameIpAgentPairs_shouldProcessAll() {
        var requests = generateRequestsWithSameIpAddressPair(5);

        Assertions.assertTrue(requests.stream().allMatch(cut::processRequest));
    }

    @Test
    void given6SameIpAgentPairs_shouldDropLastRequest() {
        var requests = generateRequestsWithSameIpAddressPair(6);


        for (int i = 0; i < requests.size() - 1; i++) {
            Assertions.assertTrue(cut.processRequest(requests.get(i)));
        }

        Assertions.assertFalse(cut.processRequest(requests.get(requests.size() - 1)));
    }

    @Test
    void givenManyOf5SameIpAgentPairsOfM_shouldProcessAllRequests() {
        var requestsTasks = IntStream.range(0, 500)
            .mapToObj(i -> generateRequestsWithSameIpAddressPair(5, i))
            .flatMap(List::stream)
            .map(toTask())
            .toList();

        try (var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
           var result = requestsTasks.stream().map(executor::submit).toList();

           var count = result.stream()
               .map(r -> {
                   try {
                       return r.get();
                   } catch (InterruptedException | ExecutionException e) {
                       throw new RuntimeException(e);
                   }
               })
               .filter(Boolean.TRUE::equals).count();

            Assertions.assertEquals(2500, count);
        }

    }

    @Test
    void givenManyOf6SameIpAgentPairsOfM_shouldDropLastRequests() {
        var requestsTasks = IntStream.range(0, 500)
            .mapToObj(i -> generateRequestsWithSameIpAddressPair(6, i))
            .flatMap(List::stream)
            .map(toTask())
            .toList();

        try (var executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            var processingResult = requestsTasks.stream().map(executor::submit).toList();

            var processedResult = processingResult.stream()
                .map(r -> {
                    try {
                        return r.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });

            var processedCount = processedResult.filter(Boolean.TRUE::equals).count();
            var droppedCount = requestsTasks.size() - processedCount;


            Assertions.assertEquals(2500, processedCount);
            Assertions.assertEquals(500, droppedCount);
        }

    }

    private Function<Request, Callable<Boolean>> toTask() {
        return r -> () -> cut.processRequest(r);
    }
    private List<Request> generateRequestsWithSameIpAddressPair(int reqNo) {
        return generateRequestsWithSameIpAddressPair(reqNo, 1);
    }

    private List<Request> generateRequestsWithSameIpAddressPair(int reqNo, int n) {
        return IntStream.range(0, reqNo)
            .mapToObj(i -> new Request("1.1.1.%d".formatted(n), "Agent X")).toList();
    }
    private List<Request> generateRequestsWithDifferentIpAgent() {
        return IntStream.range(0, 100)
            .mapToObj(i -> new Request(format("1.1.1.%d", i), format("Agent %d", i))).collect(
                Collectors.toList());
    }
}
