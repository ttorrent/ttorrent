package com.turn.ttorrent;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {
    private long time;

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(time);
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new AssertionError("not implemented");
    }
}
