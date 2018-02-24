package cz.cuni.amis.utils.lazy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maps whose items are initialized on demand by {@link #create(Object)} method upon calling {@link #get(Object)} method 
 * over the key that the map does not have a value for yet.
 * <p><p>
 * {@link #create(Object)} is called in THREAD-SAFE manner, we guarantee to call it only once per non-existing key.
 * <p><p>
 * Example use:
 * <p><p>
 * Map<String, String> lazy = new LazyMap<String, String>() {
 *   protected abstract V create(String key) {
 *     return "EMPTY";
 *   }
 * }
 * String a = lazy.get("ahoj"); // will create key under "ahoj" and fill it with "EMPTY"
 * String b = lazy.get("ahoj"); // won't call create("ahoj") again as it already have a value for it
 * if (lazy.containsKey("cau")) {
 *   // won't get here as "cau" is not within map and it won't create a new entry within a map for it as it is only "containsKey" method
 * }
 * if (lazy.containsValue("nazdar") {
 *   // won't get here for obvious reasons
 * }
 * lazy.remove("ahoj");
 * lazy.get("ahoj"); // will call create("ahoj") again!
 */
public abstract class LazyMap<K, V> implements Map<K, V> {

    /*
    This implementation locks on only one object which makes parallel access to different keys impossible.
    It is known limitation that could be solved trough a number of ways, all of which, however, have different trade-offs.
    This approach was chosen due to implementation simplicity.
     */
    private Object obj;
    private Map<K, V> store;

    /**
     * Creates value for given key. THREAD-SAFE!
     * @param key
     * @return
     */
    protected abstract V create(K key);

    public LazyMap() {
        store = new HashMap<K, V>();
        obj = new Object();
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public boolean isEmpty() {
        return store.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return store.containsKey(key);

    }

    @Override
    public boolean containsValue(Object value) {
        return store.containsValue(value);
    }

    /**
     * This method should contain "thread-safe lazy initialization" if the 'key' is not present within the map.
     */
    @Override
    public V get(Object key) {

        // Not sure if there's a better way to handle this (other than having Try and Catch for InvalidCastEx)
        K k = (K)key;
        V val;

        synchronized (obj){
            if(!store.containsKey(k)){
                val = create(k);
                val = store.put(k, val);
            }
            else {
                val = store.get(k);
            }
        }

        return val;
    }

    @Override
    public V put(K key, V value) {
        V val;
        synchronized (obj){
    	    val = store.put(key, value);
        }
        return val;
    }

    @Override
    public V remove(Object key) {
        V val;
        synchronized (obj){
            val = store.remove(key);
        }
        return val;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        synchronized (obj){
            store.putAll(m);
        }
    }

    @Override
    public void clear() {
    	store.clear();
    }

    /**
     * Should not create any new values, just return those that are already within the map.
     */
    @Override
    public Set<K> keySet() {
    	return store.keySet();
    }

    /**
     * Should not create any new values, just return those that are already within the map.
     */
    @Override
    public Collection<V> values() {
        return store.values();
    }

    /**
     * Should not create any new values, just return those that are already within the map.
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return store.entrySet();
    }

}
