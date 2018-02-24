package cz.cuni.amis.utils.flag;

import java.util.EventListener;

import cz.cuni.amis.utils.listener.Listeners;

/**
 * FULLY SPECIFIED - DO NOT TOUCH!
 * 
 * @author Jimmy
 *
 * @param <T>
 */
public interface FlagListener<T> extends EventListener {
	
	public static class FlagListenerNotifier<T> implements Listeners.ListenerNotifier<FlagListener<T>> {
		
		T value;
		
		T oldValue;
		
		public FlagListenerNotifier(T oldValue, T newValue) {
			this.oldValue = oldValue;
			this.value = newValue;
		}
		
		@Override
		public void notify(FlagListener<T> listener) {
			listener.flagChanged(oldValue, value);
		}

		@Override
		public Object getEvent() {
			return value;
		}
		
	}
	
	public void flagChanged(T oldValue, T newValue);
}
