package cz.cuni.amis.utils;

/**
 * N-argument key - used to store multiple keys within one object to provide n-argument key for maps.
 * <p><p>
 * The keys are not commutative!
 */
public class NKey {

	private int hashCode;

	public NKey(Object... keys) {
		if (keys.length == 0) {throw new IllegalArgumentException("Can't create NKey for zero elements."); }

		// Inspired by hash combining from .NET's Tuple package
		int h1 = (keys[0] != null) ? keys[0].hashCode() : 0;
		for(int i = 1; i < keys.length; i++){
		    int	h2 = (keys[i] != null) ? keys[i].hashCode() : 0;

			int rol5 = (h1 << 5) | (h1 >> 27);
			h1 = (rol5 + h1) ^ h2;
		}

		hashCode = h1;
	}                     
        
	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof NKey) || obj == null) {
			return false;
		}

		return (this.hashCode == ((NKey) obj).hashCode);
	}

}
