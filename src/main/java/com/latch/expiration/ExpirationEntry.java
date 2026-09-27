package com.latch.expiration;

import java.util.Objects;

/**
 * PriorityQueue entry storing a database key and its absolute expiration epoch timestamp.
 */
public class ExpirationEntry implements Comparable<ExpirationEntry> {
    private final String key;
    private final long expireAtEpochMilli;

    public ExpirationEntry(String key, long expireAtEpochMilli) {
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        this.expireAtEpochMilli = expireAtEpochMilli;
    }

    public String getKey() {
        return key;
    }

    public long getExpireAtEpochMilli() {
        return expireAtEpochMilli;
    }

    @Override
    public int compareTo(ExpirationEntry o) {
        return Long.compare(this.expireAtEpochMilli, o.expireAtEpochMilli);
    }
}
