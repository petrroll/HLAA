package cz.cuni.amis.utils.flag;

import java.io.Serializable;

/**
 * FULLY SPECIFIED - DO NOT TOUCH!
 * 
 * Interface for flags. Flags is a reference that raises events each time the referenced object is changed.
 */
public interface IFlag<T> extends Serializable {

	/**
	 * Adds new WEAK listener to the MAP.
	 * @param listener
	 */
    public void addWeakListener(FlagListener<T> listener);
    
    /**
	 * Adds new STRONG listener to the MAP.
	 * @param listener
	 */
    public void addStrongListener(FlagListener<T> listener);

    /**
     * Removes listener from the MAP.
     * @param listener
     */
    public void removeListener(FlagListener<T> listener);

    /**
     * Check, whether 'listener' is listening on the flag (== is used). 
     * @param listener
     * @return
     */
    public boolean isListening(FlagListener<T> listener);

    /**
     * Remove all listeners from the flag.
     */
    public void removeAllListeners();
    
    /**
     * Returns current value of the flag.
     * @return
     */
    public T getFlag();

    /**
     * Sets value to the flag. If the 'newValue' is different (!= is used) notifies all the listeners.
     * 
     * @param newValue
     */
    public void setFlag(T newValue);
}
