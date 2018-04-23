package com.kprotty.port.core;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class CycleIterator<T> implements Iterable {
    private volatile int size;
    private final List<T> items;
    private final AtomicInteger current;

    public CycleIterator() {
        size = 0;
        items = new ArrayList<>();
        current = new AtomicInteger(0);
    }

    public final int size() { return items.size(); }

    public void add(final T item) {
        items.add(item);
        size += 1;
    }

    public T get() {
        int index = current.getAndIncrement();
        current.compareAndSet(size, 0);
        if (index >= size) index = 0;
        return items.get(index);
    }

    @Override
    public Iterator<T> iterator() {
        return items.listIterator();
    }
}
