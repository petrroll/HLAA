package cz.cuni.amis.utils.flag;

import org.junit.Test;

import cz.cuni.amis.tests.BaseTest;

public class Test11_Flag_StrongListener extends BaseTest {

	public static int listened = 0;
	
	public static boolean finalized = false;
	
	public static class MyFlagListenerMock<T> extends FlagListenerMock<T> {
		
		@Override
		public void flagChanged(T oldValue, T newValue) {
			super.flagChanged(oldValue, newValue);
			++listened;
		}
		
		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			finalized = true;
		}
		
	}

	@Test
    public void test01() {
        final Flag<Boolean> flag = new Flag<Boolean>(true);
        
        flag.addStrongListener(new MyFlagListenerMock<Boolean>());        
        int count = 0;
        while (count < 10) {
        	log.info("Waiting for strong-listener (NOT) to be GC()ed... (" + (count+1) + " / 10 secs)");
        	if (finalized) break;
        	System.gc();
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted while asleep!", e);
			}
        	++count;
        }
        
        assertTrue("Strong-listener had NOT been GC()ed!", !finalized);
        assertTrue("flag.listeners.count() != 1", flag.listeners.count() == 1);
        
        log.info("Listenered was not GC()ed, correct!");
        
        flag.setFlag(false);
        flag.setFlag(true);
        
        assertTrue("Listener did not received correct number nofitifications!", listened == 2);
        
        testOk();
    }
	
}
