package cz.cuni.amis.utils;

/**
 * N-argument key - used to store multiple keys within one object to provide n-argument key for maps.
 * <p><p>
 * The keys are not commutative!
 */
public class NKey {

	// TODO: initialize this value properly
	private int hashCode;

	public NKey(Object... keys) {           
		// TODO: implement me
	}                     
        
	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO: implement me
		return false;
	}

}
