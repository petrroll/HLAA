package cz.cuni.amis.utils.flag;

import java.io.Serializable;

import cz.cuni.amis.utils.SafeEquals;
import cz.cuni.amis.utils.listener.Listeners;

/**
 * This class may be used to create an observable value (you may attach change-listeners to it).
 * <p><p> 
 * This is flag class which is designed for Boolean or Integer types (but 
 * it should work with other types as well as long as they have equals() implemented
 * correctly).
 * <p><p>
 * It allows you to store the state of flag and register listeners on the flag.
 * <p><p>
 * Note that the implementation is:
 * <ol>
 * <li>thread-safe (truly),</li>
 * <li>recursion-safe (meaning that the flag may be changed from within the listener it notifies of the flag changes - such events are put into the queue and processed in correct sequence),</li>
 * <li>setters/getters are non-blocking (or they blocks for finite small amount of time ~ few synchronized statements used, which can't block each other for greater period of time).</li>
 * </ol>
 * <p><p>
 * Also note that can't be a really correct implementation of the flag that always returns
 * the right value - if you heavily use flag from let's say a tens of threads then there may
 * be glitches in the getFlag() returned value (but for the most implementation this value
 * will be correct in 99.99999%!).
 * <p><p>
 * Note that the implementation of notifying about flag-change is
 * strictly time-ordered. Every flag-change event is fully processed before another is raised/received.
 * There is a possibility that a listener on the flag change will attempt to change the flag again (mentioned recursion-safe).
 * In that case processing of this flag change is postponed until the previous event has been fully processed
 * (e.g. all listeners has been notified about it).
 * <p><p>
 * Last piece of magic - if you want to change the flag value in-sync (meaning that you need 100% safe reading of the flag value), instantiate Flag.DoInSync<T> class
 * and submit it via inSync() method - {@link DoInSync#execute(Object)} method will be executed in synchronized state so no one can change the flag value
 * while you're inside this method. 
 * 
 * @author Jimmy
 *
 * @param <T> type of the flag
 */
public class Flag<T> implements IFlag<T>, Serializable {

	Listeners<FlagListener<T>> listeners = new Listeners<FlagListener<T>>();
	
	/**
	 * Do not read directly - always use getFlag() method.
	 */
    T value;
    
    
    /**
     * Mutex that we synchronized on when the result of getValue() should be changed.
     */
    Object setMutex = new Object();
    
    /**
     * Initialize the flag with 'null' as an initial value.
     */
    public Flag() {
        value = null;
    }

    /**
     * Initialize the flag with 'initialValue'.
     * @param initialValue
     */
    public Flag(T initialValue) {
        value = initialValue;
    }
    
    /**
     * Changes the flag and informs all listeners.
     * 
     * @param newValue
     * @throws InterruptedRuntimeException if interrupted during the await on the freeze latch
     */
    @Override
    public void setFlag(T newValue) {
    	synchronized(setMutex) {
	    	if (SafeEquals.equals(value, newValue)) return;
	    	T oldValue = value;
	    	this.value = newValue;
	    	FlagListener.FlagListenerNotifier<T> notifier = new FlagListener.FlagListenerNotifier<T>(oldValue, newValue);
	        listeners.notify(notifier);
    	}
    }
    
    /**
     * Returns the value of the flag.
     * <p><p>
     * Note that if the flag contains any set-flag pending requests queue it will return the last
     * value from this queue.
     * <p><p>
     * This has a big advantage for the multi-thread heavy-listener oriented designs.
     * <p>
     * Every time a listener is informed about the flag change it receives a new value of the flag
     * but additionally it may query the flag for the last value there will be set into it.
     * <p>
     * Note that if you use the Flag sparingly this mechanism won't affect you in 99.99999% of time.
     * <p><p>
     * Warning - this method won't return truly a correct value if you will use inSync() method because
     * this time we won't be able to obtain the concrete value of the flag after the DoInSync command
     * will be carried out - instead we return the first value we are aware of. Again this won't
     * affect you in any way (... but you should know such behavior exist ;-)) 
     * 
     * @return value of the flag
     */
    public T getFlag() {
    	synchronized(setMutex) {
	    	return value;	    	    
    	}
    }
    
    /**
     * Adds new listener to the flag (strong reference).
     * <BR><BR>
     * Using this method is memory-leak prone.
     * 
     * @param listener
     */
    @Override
    public void addStrongListener(FlagListener<T> listener) {
        if (listener == null) {
            return;
        }
        listeners.addStrongListener(listener);
    }
        
    /**
     * Adds new listener to the flag with specified param. It will weak-reference the listener so when
     * you drop the references to it, it will be automatically garbage-collected.
     * <p><p>
     * Note that all anonymous
     * listeners are not subject to gc() because they are reachable from within the object where they were
     * created.
     * 
     * @param listener
     */
    @Override
    public void addWeakListener(FlagListener<T> listener) {
    	if (listener == null) {
            return;
        }
    	listeners.addWeakListener(listener);
    }
    
    /**
     * Removes all registered 'listener' from the flag.
     * @param listener
     */
    @Override
    public void removeListener(FlagListener<T> listener) {
        if (listener == null) {
            return;
        }
        listeners.removeListener(listener);        
    }

    /**
     * Removes all listeners.
     */
    @Override
    public void removeAllListeners() {
        listeners.clearListeners();
    }

    /**
     * Checks whether listener is already registered (using equals()).
     * <BR><BR>
     * @param listener
     * @return true if listener is already registered
     */
    @Override
    public boolean isListening(FlagListener<T> listener) {
        if (listener == null) {
            return false;
        }
        return listeners.isListening(listener);
    }

}

