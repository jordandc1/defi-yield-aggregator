package app.dya.price;

import java.time.*;
import java.util.concurrent.ConcurrentHashMap;

public class TimedCache<K,V> {
    static final class Entry<V>{ final V v; final Instant exp; Entry(V v, Instant exp){ this.v=v; this.exp=exp; } }
    private final Duration ttl;
    private final ConcurrentHashMap<K, Entry<V>> map = new ConcurrentHashMap<>();

    public TimedCache(Duration ttl){ this.ttl = ttl; }

    public V get(K key){
        var e = map.get(key);
        if(e==null) return null;
        if(Instant.now().isAfter(e.exp)){ map.remove(key, e); return null; }
        return e.v;
    }
    public void put(K key, V val){ map.put(key, new Entry<>(val, Instant.now().plus(ttl))); }
}
