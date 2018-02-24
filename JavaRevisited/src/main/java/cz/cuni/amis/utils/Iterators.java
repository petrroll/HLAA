package cz.cuni.amis.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * This class allows you to combine several iterators in single one allowing you to seamlessly iterate over several
 * collections at once.
 * <p><p>
 * This class behaves as defined by {@link Iterator} contract.
 * 
 * @author Jimmy
 *
 * @param <NODE>
 */
public class Iterators<NODE> implements Iterator<NODE> {
	int currentIteratorIndex;
	Iterator<NODE>[] iterators;

	/**
	 * Initialize this class to use "iterators" in the order as they are passed into the constructor.
	 * @param iterators may contain nulls
	 */
	public Iterators(Iterator<NODE>... iterators) {
		this.iterators = iterators;
		currentIteratorIndex = 0;
	}
	
	@Override
	public boolean hasNext() {

		// save original iterator index, try to move to next element, restore it to original state
		// (a remove might want to remove the element returned by the last 'next()'.
		int originalIteratorIndex = this.currentIteratorIndex;
	    boolean isThereNextOnNextValidIterator = moveToNextValidIterator();
	    this.currentIteratorIndex = originalIteratorIndex;

		return isThereNextOnNextValidIterator;

	}

	private boolean moveToNextValidIterator() {

		while(currentIteratorIndex < iterators.length) {
			if (iterators[currentIteratorIndex] != null && iterators[currentIteratorIndex].hasNext()) {
				return true;
			}
			currentIteratorIndex++;
		}

		return false;
	}

	@Override
	public NODE next() {

		if(moveToNextValidIterator()){
		 	return iterators[currentIteratorIndex].next();
		}

		throw new NoSuchElementException("No next element in Iterator");
	}

	@Override
	public void remove() {
		if(!isValidIteratorSelected()) {
			throw new IllegalStateException("No element to remove.");
		}
		iterators[currentIteratorIndex].remove();
	}

	private boolean isValidIteratorSelected(){
		return (currentIteratorIndex < iterators.length && iterators[currentIteratorIndex] != null);
	}

}
