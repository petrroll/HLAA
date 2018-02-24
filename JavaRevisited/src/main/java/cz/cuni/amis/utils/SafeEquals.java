package cz.cuni.amis.utils;

public class SafeEquals {
	
	/**
	 * Uses {@link Object#equals(Object)}, to compare both objects.
	 * <p><p>
	 * In case that both objects are null, returns true.
	 * <p><p>
	 * If only one object is null, returns false.
	 *  
	 * @param o1 may be null
	 * @param o2 may be null
	 * @return
	 */
	public static boolean equals(Object o1, Object o2) {
		if(o1 == null && o2 == null){
			return true;
		}
		else if( o1 == null ^ o2 == null){
			return false;
		}
		else {
			return o1 == o2;
		}
	}

}
