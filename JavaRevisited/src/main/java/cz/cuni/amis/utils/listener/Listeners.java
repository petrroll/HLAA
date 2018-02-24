package cz.cuni.amis.utils.listener;

import java.lang.ref.WeakReference;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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

    // We could (and we probably should) do a more fine grained locking
    // since the purpose of this assignment is to teach us about weak reference, however,
    // it was decided to not to bother.
    Object lckWk;
    Object lckSt;

    LinkedList<Listener> strongListeners;
    LinkedList<WeakReference<Listener>> weakListeners;

    public Listeners(){
        strongListeners = new LinkedList<Listener>();
        weakListeners = new LinkedList<WeakReference<Listener>>();

        lckWk = new Object();
        lckSt = new Object();
    }

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
        synchronized (lckSt){
            strongListeners.add(listener);
        }
    }
    
    /**
     * Adds listener with weak reference to it.
     * @param listener
     */
    public void addWeakListener(Listener listener) {
        synchronized (lckWk){
            weakListeners.add(new WeakReference<Listener>(listener));
        }
    }
    
    /**
     * Removes all listeners that are == to this one (not equal()! must be the same object).
     * @param listener
     * @return how many listeners were removed
     */
    public int removeListener(EventListener listener) {
        int numberOfRemoved = 0;

        synchronized (lckSt) {
            Iterator<Listener> iter = strongListeners.iterator();
            while (iter.hasNext()) {

                Listener list = iter.next();
                if(list == listener) {
                    iter.remove();
                    numberOfRemoved++;
                }

            }
        }

        synchronized (lckWk){
            Iterator<WeakReference<Listener>> iter = weakListeners.iterator();
            while (iter.hasNext()) {

                WeakReference<Listener> listRef = iter.next();
                Listener list = listRef.get();

                if(list == listener) {
                    iter.remove();
                    numberOfRemoved++;
                } else if(list == null) {
                    iter.remove();
                }

            }
        }

    	return numberOfRemoved;
    }
    
    /**
     * Calls notifier.notify() on each of the stored listeners, allowing you to execute stored
     * command.
     * 
     * @param notifier
     */
    public void notify(ListenerNotifier<Listener> notifier) {
        synchronized (lckSt){
            for (Listener list:strongListeners) {
                notifier.notify(list);
            }
        }

        synchronized (lckWk){
            Iterator<WeakReference<Listener>> iter = weakListeners.iterator();
            while (iter.hasNext()) {

                WeakReference<Listener> listRef = iter.next();
                Listener list = listRef.get();

                if(list == null) {
                    iter.remove();
                } else{
                    notifier.notify(list);
                }

            }
        }
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
        synchronized (lckSt){
            for (Listener list:strongListeners) {
                if(list == listener) { return true; }
            }
        }

        synchronized (lckWk){
            Iterator<WeakReference<Listener>> iter = weakListeners.iterator();
            while (iter.hasNext()) {

                WeakReference<Listener> listRef = iter.next();
                Listener list = listRef.get();

                if(list == null) {
                    iter.remove();
                } else{
                    if (list == listener) { return true; }
                }

            }
        }

        return false;
    }
    
    public void clearListeners() {
    	synchronized (lckSt){
    	    strongListeners.clear();
        }

        synchronized (lckWk){
    	    weakListeners.clear();
        }
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

        // despite the doc-comment the assignment's test expects this to reflect the true number of valid elements
        // I.e. one has to do null-weak-references purging first
        synchronized (lckWk){
            Iterator<WeakReference<Listener>> iter = weakListeners.iterator();
            while (iter.hasNext()) {

                WeakReference<Listener> listRef = iter.next();
                Listener list = listRef.get();

                if(list == null) {
                    iter.remove();
                }
            }
        }

        return weakListeners.size() + strongListeners.size();
    }
    
}
