package cz.cuni.amis.utils.flag;

import org.junit.Test;

import cz.cuni.amis.tests.BaseTest;

public class Test10_Flag_WeakListener extends BaseTest {

	public static int listened = 0;
	
	public static boolean finalized = false;

	/**
     * Not so good test of the flag - you need to confirm the results for yourself.
     */
	@Test
    public void test01() {
        final Flag<Boolean> flag = new Flag<Boolean>(true);
        
        FlagListener<Boolean> myListener = new FlagListener<Boolean>() {

			@Override
			public void flagChanged(Boolean oldValue, Boolean newValue) {
				++listened;
			}
			
			@Override
			protected void finalize() throws Throwable {
				super.finalize();
				finalized = true;
			}
        	
        }; 
        
        flag.addWeakListener(myListener);
        
        flag.setFlag(false);
        flag.setFlag(true);
        
        assertTrue("Weak listener has not been notified!", listened == 2);
        assertTrue("flag.listeners.count() != 1", flag.listeners.count() == 1);
        
        log.info("Dropping weak listener reference...");
        myListener = null;        
        
        int count = 0;
        while (count < 60) {
        	log.info("Waiting for weak-listener to be GC()ed... (" + (count+1) + " / 60 secs)");
        	if (finalized) break;
        	System.gc();
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted while asleep!", e);
			}
        	++count;
        }
        
        assertTrue("Weak-listener has not been GC()ed!", finalized);
        assertTrue("flag.listeners.count() != 0", flag.listeners.count() == 0);
        
        log.info("Listenered GC()ed OK!");
        
        listened = 0;
        flag.setFlag(false);
        flag.setFlag(true);
        
        assertTrue("Listener still received notification!", listened == 0);
        assertTrue("flag.listeners.count() != 0", flag.listeners.count() == 0);
        
        testOk();
    }
	
}
