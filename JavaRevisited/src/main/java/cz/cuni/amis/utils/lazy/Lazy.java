package cz.cuni.amis.utils.lazy;

/**
 * Utility class for lazy initialization of objects.
 * <p><p>
 * {@link #create(Object)} is called in THREAD-SAFE manner, we guarantee to call it only once.
 */
public abstract class Lazy<T> {

   /**
     * Creates lazy initialized object.
     * @return
     */
    abstract protected T create();

    /**
     * Once call will construct new object via {@link #create()}, successive calls
     * will return it. That is {@link #create()} may be called only once during
     * the life-time of the object ... ensure THREAD-SAFETY!
     * @return
     */
    public T get() {
        // Let's try fast path without any synchronization
        if (initialized) { return constructedObject;}

        synchronized (O){
            if(!initialized) {
                constructedObject = create();
                initialized = true;
            }
        }
        return constructedObject;
    }

    private Object O = new Object();
    private T constructedObject = null;
    private boolean initialized = false;


}
