package com.zenvora.events.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ActiveEvent {
    private final EventDefinition definition;
    private final long endAt;
    private final Map<UUID, Integer> scores = new ConcurrentHashMap<>();

    public ActiveEvent(EventDefinition definition) {
        this.definition = definition;
        this.endAt = System.currentTimeMillis() + definition.durationSeconds() * 1000L;
    }

    public EventDefinition definition() { return definition; }
    public long endAt() { return endAt; }
    public Map<UUID, Integer> scores() { return scores; }

    public void addScore(UUID uuid, int points) {
        if (points > 0) scores.merge(uuid, points, Integer::sum);
    }

    public int score(UUID uuid) {
        return scores.getOrDefault(uuid, 0);
    }

    public boolean expired() {
        return System.currentTimeMillis() >= endAt;
    }
}
