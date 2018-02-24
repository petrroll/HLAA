package cz.cuni.amis.utils;

public class NullCheck {
	
	/**
	 * Throws {@link IllegalArgumentException} if obj == null. Used during the construction of the objects.
	 * @param obj
	 */
	public static void check(Object obj) {
		if(obj == null) { throw new IllegalArgumentException("Argument obj is null."); }
	}

}
