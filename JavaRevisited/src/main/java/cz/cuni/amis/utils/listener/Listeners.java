package cz.cuni.amis.utils.listener;

import java.util.EventListener;

/**
 * This object is implementing listeners list, where you may store both type of references to 
 * the listeners (strong reference / weak reference).
 * <BR><BR>
 * It takes quite of effort to maintain the list with both strong / weak references,
 * therefore the class was created.
 * <BR><BR>
 * Because we don't know what method we will be calling on the listeners the public
 * interface ListenerNotifier exists. If you want to fire all the listeners, just
 * implement this interface and stuff it to the notify() method of the "Listeners".
 * (This somehow resembles the Stored Procedure pattern...)
 * <BR><BR>
 * The class is fully THREAD-SAFE.
 * 
 * @author Jimmy
 */
public class Listeners<Listener extends EventListener> {	
	
	/**
	 * Used to raise the event in the listeners.
	 * 
	 * @author Jimmy
	 *
	 * @param <Listener>
	 */
	public static interface ListenerNotifier<Listener extends EventListener> {
		
		public Object getEvent();
		
		public void notify(Listener listener);
		
	}
	
	/**
     * Adds listener with strong reference to it.
     * @param listener
     */
    public void addStrongListener(Listener listener) {
    	// TODO: implement me!
    }
    
    /**
     * Adds listener with weak reference to it.
     * @param listener
     */
    public void addWeakListener(Listener listener) {
    	// TODO: implement me!
    }
    
    /**
     * Removes all listeners that are == to this one (not equal()! must be the same object).
     * @param listener
     * @return how many listeners were removed
     */
    public int removeListener(EventListener listener) {
    	// TODO: implement me!
    	return 0;
    }
    
    /**
     * Calls notifier.notify() on each of the stored listeners, allowing you to execute stored
     * command.
     * 
     * @param notifier
     */
    public void notify(ListenerNotifier<Listener> notifier) {
    	// TODO: implement me!    	    	
    }
    
    /**
     * Returns true if at least one == listener to the param 'listener' is found.
     * <BR><BR>
     * Not using equal() but pointer ==.
     * 	 
     * @param listener
     * @return
     */
    public boolean isListening(EventListener listener) {
    	// TODO: implement me!
    	return false;
    }
    
    public void clearListeners() {
    	// TODO: implement me!
    }
    
    /**
     * Returns count of listeners in the list, note that this may not be exact as we store also
     * listeners with weak listeners, but the list will be purged in next opportunity (like raising
     * event, removing listener).
     * <p><p>
     * Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of used queue, determining the current
     * number of elements requires an O(n) traversal.
     * 
     * @return
     */
    public int count() {
    	// TODO: implement me!
    	return 0;
    }
    
}
