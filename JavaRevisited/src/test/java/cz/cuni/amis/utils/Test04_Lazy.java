package cz.cuni.amis.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import cz.cuni.amis.tests.ConcurrencyTest;
import cz.cuni.amis.tests.ConcurrentSyncTask;
import cz.cuni.amis.tests.ConcurrentTask;
import cz.cuni.amis.utils.lazy.Lazy;

public class Test04_Lazy extends ConcurrencyTest {

	private static final int COUNT = 1000;
	
	private volatile AtomicInteger createCall = new AtomicInteger(0);
	
	private volatile AtomicInteger createNullCall = new AtomicInteger(0);
	
	private Lazy<Integer> lazy = new Lazy<Integer>() {
		@Override
		protected Integer create() {
			if (createCall.addAndGet(1) > 1) {
				throw new RuntimeException("Invalid number of createCall() made... > 1");
			}
			return 1;
		}
		
	};
	
	private Lazy<Integer> lazyNull = new Lazy<Integer>() {
		@Override
		protected Integer create() {
			if (createNullCall.addAndGet(1) > 1) {
				throw new RuntimeException("Invalid number of createCall() made... > 1");
			}
			return null;
		}
		
	};
	
	private class GetJob extends ConcurrentSyncTask {

		private Lazy<?> myLazy;

		public GetJob(Lazy<?> myLazy) {
			this.myLazy = myLazy;
		}
		
		@Override
		protected void runImpl() {
			for (int i = 0; i < COUNT; ++i) {
				myLazy.get();
			}
		}
		
	}
	
	@Test
	public void test01() {
		
		int threadCount = 10;
		
		List<ConcurrentSyncTask> tasks = new ArrayList<ConcurrentSyncTask>();
		
		for (int i = 0; i < threadCount; ++i) {
			tasks.add(new GetJob(lazy));
		}
		
		runConcurrentSyncStart(tasks);
		
		log.info("Checking results...");
		
		assertTrue("lazy.get() == must be == 1 !!!", lazy.get() == 1);
		
		testOk();	
	}	
	
	@Test
	public void test02() {
		
		int threadCount = 10;
		
		List<ConcurrentSyncTask> tasks = new ArrayList<ConcurrentSyncTask>();
		
		for (int i = 0; i < threadCount; ++i) {
			tasks.add(new GetJob(lazyNull));
		}
		
		runConcurrentSyncStart(tasks);
		
		log.info("Checking results...");
		
		assertTrue("lazy.get() == must be == null !!!", lazyNull.get() == null);
		
		testOk();	
	}
	
}
