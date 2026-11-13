package com.council.tenderapp.operator.reset;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ResetCoordinator {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public Guard request() { return acquire(lock.readLock()); }
    public Guard reset() { return acquire(lock.writeLock()); }

    private Guard acquire(Lock selected) {
        selected.lock();
        return new Guard(selected);
    }

    public record Guard(Lock lock) implements AutoCloseable {
        @Override
        public void close() { lock.unlock(); }
    }
}
