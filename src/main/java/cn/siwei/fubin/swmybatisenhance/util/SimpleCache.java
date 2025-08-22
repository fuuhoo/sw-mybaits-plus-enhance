package cn.siwei.fubin.swmybatisenhance.util;

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SimpleCache<K, V> implements Iterable<Map.Entry<K, V>>, Serializable {
    private static final long serialVersionUID = 1L;

    // 可变的键包装类
    private static class Mutable<K> {
        private final K value;

        private Mutable(K value) {
            this.value = value;
        }

        public static <K> Mutable<K> of(K value) {
            return new Mutable<>(value);
        }

        public K get() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Mutable<?> that = (Mutable<?>) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }

    // 弱引用并发映射
    private static class WeakConcurrentMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
        private final ConcurrentMap<WeakReference<K>, V> map = new ConcurrentHashMap<>();
        private final ReferenceQueue<K> queue = new ReferenceQueue<>();

        private void cleanStaleEntries() {
            WeakReference<?> ref;
            while ((ref = (WeakReference<?>) queue.poll()) != null) {
                map.remove(ref);
            }
        }

        @Override
        public V get(Object key) {
            cleanStaleEntries();
            return map.get(new WeakReference<>(key));
        }

        @Override
        public V put(K key, V value) {
            cleanStaleEntries();
            return map.put(new WeakKey<>(key, queue), value);
        }

        @Override
        public V remove(Object key) {
            cleanStaleEntries();
            return map.remove(new WeakReference<>(key));
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public V putIfAbsent(K key, V value) {
            cleanStaleEntries();
            return map.putIfAbsent(new WeakKey<>(key, queue), value);
        }

        @Override
        public boolean remove(Object key, Object value) {
            cleanStaleEntries();
            return map.remove(new WeakReference<>(key), value);
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            cleanStaleEntries();
            return map.replace(new WeakReference<>(key), oldValue, newValue);
        }

        @Override
        public V replace(K key, V value) {
            cleanStaleEntries();
            return map.replace(new WeakReference<>(key), value);
        }

        private static class WeakKey<T> extends WeakReference<T> {
            private final int hashCode;

            WeakKey(T referent, ReferenceQueue<? super T> q) {
                super(referent, q);
                hashCode = Objects.hashCode(referent);
            }

            @Override
            public int hashCode() {
                return hashCode;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof WeakReference)) return false;

                Object o1 = get();
                Object o2 = ((WeakReference<?>) obj).get();

                if (o1 == null || o2 == null) return false;
                return Objects.equals(o1, o2);
            }
        }
    }

    private final Map<Mutable<K>, V> rawMap;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected final Map<K, Lock> keyLockMap = new ConcurrentHashMap<>();

    public SimpleCache() {
        this(new WeakConcurrentMap<>());
    }

    public SimpleCache(Map<Mutable<K>, V> initMap) {
        this.rawMap = initMap;
    }

    public V get(K key) {
        lock.readLock().lock();
        try {
            return rawMap.get(Mutable.of(key));
        } finally {
            lock.readLock().unlock();
        }
    }

    public V get(K key, Supplier<V> supplier) {
        return get(key, null, supplier);
    }

    public V get(K key, Predicate<V> validPredicate, Supplier<V> supplier) {
        V v = get(key);

        // 验证缓存值有效性
        if (validPredicate != null && v != null && !validPredicate.test(v)) {
            v = null;
        }

        if (v == null && supplier != null) {
            // 获取键级锁
            Lock keyLock = keyLockMap.computeIfAbsent(key, k -> new ReentrantLock());
            keyLock.lock();
            try {
                // 双重检查
                v = get(key);
                if (v == null || (validPredicate != null && !validPredicate.test(v))) {
                    v = supplier.get();
                    put(key, v);
                }
            } finally {
                keyLock.unlock();
                keyLockMap.remove(key);
            }
        }
        return v;
    }

    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            rawMap.put(Mutable.of(key), value);
            return value;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public V remove(K key) {
        lock.writeLock().lock();
        try {
            return rawMap.remove(Mutable.of(key));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            rawMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        lock.readLock().lock();
        try {
            List<Map.Entry<K, V>> entries = new ArrayList<>();
            for (Map.Entry<Mutable<K>, V> entry : rawMap.entrySet()) {
                entries.add(new Map.Entry<K, V>() {
                    @Override
                    public K getKey() {
                        return entry.getKey().get();
                    }

                    @Override
                    public V getValue() {
                        return entry.getValue();
                    }

                    @Override
                    public V setValue(V value) {
                        throw new UnsupportedOperationException();
                    }
                });
            }
            return entries.iterator();
        } finally {
            lock.readLock().unlock();
        }
    }
}