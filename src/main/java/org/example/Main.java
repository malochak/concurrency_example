package org.example;

import org.example.model.Request;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {

    private final Map<Request, List<LocalDateTime>> requestsByTime = new HashMap<>();
    private final Clock clock;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Main(Clock clock) {
        this.clock = clock;
    }

    public boolean processRequest(Request request) {
        System.out.printf("Request with ip/agent (%s/%s) is being processed%n", request.ip(),
            request.userAgent());
        return canBeProcessed(request);
    }

    private synchronized boolean canBeProcessed(Request request) {
        List<LocalDateTime> lastRequestsTimes = requestsByTime.get(request);

        if (lastRequestsTimes != null) {
            var now = LocalDateTime.now(clock);

            var histsWithinLastTenMinutes =
                lastRequestsTimes.stream().filter(time -> now.minusMinutes(10).isBefore(time))
                    .count();

            if (histsWithinLastTenMinutes >= 5) {
                return false;
            }

            lastRequestsTimes.add(LocalDateTime.now(clock));

            return true;
        }

        requestsByTime.put(request, new ArrayList<>(List.of(LocalDateTime.now())));

        return true;
    }
}
