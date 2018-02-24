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
	
	/**
	 * Initialize this class to use "iterators" in the order as they are passed into the constructor.
	 * @param iterators may contain nulls
	 */
	public Iterators(Iterator<NODE>... iterators) {
		// TODO: implement me!
	}
	
	@Override
	public boolean hasNext() {
		// TODO: implement me!
		return false;
	}

	@Override
	public NODE next() {
		// TODO: implement me!
		return null;
	}

	@Override
	public void remove() {
		// TODO: implement me!
	}

}
