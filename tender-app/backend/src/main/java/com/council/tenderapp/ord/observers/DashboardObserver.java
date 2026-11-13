package com.council.tenderapp.ord.observers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.council.tenderapp.ord.events.OrderStatusEvent;

@Component
public class DashboardObserver implements OrderObserver {

    private static final Logger logger = LoggerFactory.getLogger(DashboardObserver.class);

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> subscribers =
        new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long restaurantId) {
        SseEmitter emitter = new SseEmitter(0L);
        CopyOnWriteArrayList<SseEmitter> restaurantSubscribers =
            subscribers.computeIfAbsent(restaurantId, ignored -> new CopyOnWriteArrayList<>());
        restaurantSubscribers.add(emitter);

        emitter.onCompletion(() -> removeSubscriber(restaurantId, emitter));
        emitter.onTimeout(() -> {
            removeSubscriber(restaurantId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> removeSubscriber(restaurantId, emitter));
        return emitter;
    }

    @Override
    public void onOrderStatusChanged(OrderStatusEvent event) {
        List<SseEmitter> restaurantSubscribers = subscribers.get(event.restaurantId());
        if (restaurantSubscribers == null) {
            return;
        }

        for (SseEmitter emitter : restaurantSubscribers) {
            try {
                emitter.send(SseEmitter.event().id(event.eventId()).data(event));
            } catch (IOException exception) {
                removeSubscriber(event.restaurantId(), emitter);
                emitter.completeWithError(exception);
                logger.warn(
                    "Unable to notify a dashboard observer for restaurant {} about {}",
                    event.restaurantId(),
                    event.eventId(),
                    exception
                );
            }
        }
    }

    public void completeAll() {
        subscribers.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));
        subscribers.clear();
    }

    private void removeSubscriber(Long restaurantId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> restaurantSubscribers = subscribers.get(restaurantId);
        if (restaurantSubscribers == null) {
            return;
        }

        restaurantSubscribers.remove(emitter);
        if (restaurantSubscribers.isEmpty()) {
            subscribers.remove(restaurantId, restaurantSubscribers);
        }
    }
}
