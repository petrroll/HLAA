package cz.cuni.amis.utils.flag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import cz.cuni.amis.tests.ConcurrencyTest;
import cz.cuni.amis.tests.ConcurrentTask;

public class Test12_FlagConcurrency3 extends ConcurrencyTest {

	public static final int COUNT = 1000;
	
	public static boolean[] finalized;
	
	@SuppressWarnings("rawtypes")
	public static Set[] listened;
	
	public static class MyFlagListenerMock<T> extends FlagListenerMock<T> {
		
		private int myNumber;

		public MyFlagListenerMock(int listenerNum) {
			this.myNumber = listenerNum;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void flagChanged(T oldValue, T newValue) {
			super.flagChanged(oldValue, newValue);
			listened[myNumber].add(newValue);			
		}
		
		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			finalized[myNumber] = true;
		}
		
	}
	
	private Flag<Integer> flag = new Flag<Integer>(-1);
	
	private class SetFlagJob extends ConcurrentTask {
		
		private MyFlagListenerMock<Integer> myListener;
		
		private int from;

		private int to;

		private int threadNum;

		private Integer[] checkValues;
		
		public SetFlagJob(int threadNum, int from, int to) {
			this.from = from;
			this.to = to;
			this.threadNum = threadNum;
			checkValues = new Integer[to-from];
			for (int i = 0; i < checkValues.length; ++i) {
				checkValues[i] = from + i;
			}			
		}

		@Override
		protected void runImpl() {
			// ADD WEAK LISTENER
			finalized[threadNum] = false;
		    listened[threadNum] = new HashSet<Integer>();
		    
		    myListener = new MyFlagListenerMock<Integer>(threadNum);
			flag.addWeakListener(myListener);
			
			for (int i = from; i < to; ++i) {
		       	flag.setFlag(i);
		    }
			
			assertTrue("Weak Listener has not received notifications!", listened[threadNum].size() >= to-from);
			
			myListener = null; // DROPPING WEAK-LISTENER REFERENCE
			listened[threadNum].clear();
			
			// WAIT FOR WEAK-LISTENER TO BE GC()ed
			
			int count = 0;
	        while (count < 60) {
	        	log.info("Waiting for weak-listener to be GC()ed... (" + (count+1) + " / 60 secs)");
	        	if (finalized[threadNum]) break;
	        	System.gc();
	        	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException("Interrupted while asleep!", e);
				}
	        	++count;
	        }
	        
	        assertTrue("Weak-listener has not been GC()ed!", finalized[threadNum]);
	        
	        log.info("Listenered GC()ed OK!");
	        
	        for (int i = from; i < to; ++i) {
	        	flag.setFlag(i);
	        }
	        
	        for (int i = from; i < to; ++i) {
	        	if (listened[threadNum].contains(i)) {
	        		throw new RuntimeException("Listener was notified even after it was GC()ed!");
	        	}
	        }	        
	        
	        finalized[threadNum] = false;
	        listened[threadNum].clear();
	        
	        // ADD STRONG LISTENER
	        flag.addStrongListener(new MyFlagListenerMock<Integer>(threadNum));        
	        count = 0;
	        while (count < 10) {
	        	log.info("Waiting for strong-listener (NOT) to be GC()ed... (" + (count+1) + " / 10 secs)");
	        	if (finalized[threadNum]) break;
	        	System.gc();
	        	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException("Interrupted while asleep!", e);
				}
	        	++count;
	        }
	        
	        
	        assertTrue("Strong-listener had been GC()ed!", !(finalized[threadNum]));
	        
	        log.info("Listenered was not GC()ed, correct!");
	        
	        for (int i = from; i < to; ++i) {
	        	flag.setFlag(i);
	        }
	        
	        assertTrue("Listener did not received correct number nofitifications (listener still received some notifications(s) after flag.removeAllListeners())!", listened[threadNum].size() >= to-from);
		}
		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test01() {
		int threadCount = 30;
		
		finalized = new boolean[threadCount];
		listened = new HashSet[threadCount];
		
		List<SetFlagJob> tasks = new ArrayList<SetFlagJob>();
		
		for (int i = 0; i < threadCount; ++i) {
			tasks.add(new SetFlagJob(i, i*COUNT, (i+1)*COUNT));
		}
		
		runConcurrent((List<ConcurrentTask>)(List)tasks, threadCount);
		
		testOk();
	}
	
}
