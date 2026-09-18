package com.zenvora.events.event;

import org.bukkit.Material;

import java.util.Map;

public record EventDefinition(
        String id,
        String displayName,
        String description,
        boolean enabled,
        long durationSeconds,
        long cooldownSeconds,
        EventType type,
        int defaultPoints,
        Map<String, Integer> valuablePoints,
        Map<String, java.util.List<String>> rewards
) {
    public int pointsFor(String key) {
        return valuablePoints.getOrDefault(key.toUpperCase(), defaultPoints);
    }
}
