package cz.cuni.amis.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import cz.cuni.amis.tests.ConcurrencyTest;
import cz.cuni.amis.tests.ConcurrentTask;
import cz.cuni.amis.utils.lazy.LazyMap;

public class Test05_LazyMap extends ConcurrencyTest {

	private static final int COUNT = 10000;
	
	private volatile AtomicInteger createCall = new AtomicInteger(0);
	
	private Map<Integer, Integer> ints = new LazyMap<Integer, Integer>() {

		@Override
		protected Integer create(Integer key) {
			if (createCall.addAndGet(1) > COUNT) {
				throw new RuntimeException("Invalid number of LazyMap.create() calls made!");
			}
			return key;
		}		
	};
	
	private int get(int i) {
		
		Integer result = ints.get(i);
		
		if (result != null) {
			return result;
		}
		
		log.info(Thread.currentThread().getName() + ": get(" + i + ") regetting...");
		result = ints.get(i);
		if (result != null) {
			log.info(Thread.currentThread().getName() + ": get(" + i + ") sync get success!");
			return result;
		}
		log.info(Thread.currentThread().getName() + ": get(" + i + ") inserting...");
		throw new RuntimeException("SHOULD NOT REACH HERE! We called map.get(" + i + ") and it did not returned any number!");
	}
	
	
	
	private class InsertJob extends ConcurrentTask {

		@Override
		protected void runImpl() {
			for (int i = 0; i < COUNT; ++i) {
				get(i);
			}
		}
		
	}
	
	@Test
	public void test() {	
		int threadCount = 30;
		
		List<ConcurrentTask> tasks = new ArrayList<ConcurrentTask>();
		
		log.warning("Going to stress the thread safety of LazyMap!");
		log.warning("Inserting " + COUNT + " numbers into the LazyMap concurrently using " + threadCount + " threads! That is, we're calling get(0-" + (COUNT-1) + ") concurrently from " + threadCount + " threads.");
		
		
		for (int i = 0; i < threadCount; ++i) {
			tasks.add(new InsertJob());
		}
		
		runConcurrent(tasks, threadCount);
		
		log.info("Checking results...");
		
		// CHECK RESULTS
		
		Integer[] keys = ints.keySet().toArray(new Integer[COUNT]);
		Integer[] values = ints.values().toArray(new Integer[COUNT]);
		
		if (keys.length != COUNT) {
			testFailed("keys.length != " + COUNT + " == number of inserted pairs");
		}
		if (values.length != COUNT) {
			testFailed("values.length != " + COUNT + " == number of inserted pairs");			
		}
		
		// NULL CHECKS
		
		for (int i = 0; i < keys.length; ++i) {
			if (keys[i] == null) {
				testFailed("keys[" + i + "] == null");
			}
		}
		
		for (int i = 0; i < values.length; ++i) {
			if (values[i] == null) {
				testFailed("values[" + i + "] == null");
			}
		}
		
		Arrays.sort(keys);
		Arrays.sort(values);
		
		for (int i = 0; i < COUNT; ++i) {
			if (keys[i] != i) {
				testFailed("keys[i] == " + keys[i] + " != " + i + " which is expected value");
			}
			if (values[i] != i) {
				testFailed("values[i] == " + values[i] + " != " + i + " which is expected value");
			}
		}
		
		Integer[] keyRegs = new Integer[COUNT];
		Integer[] valueRegs = new Integer[COUNT];
		for (int i = 0; i < COUNT; ++i) {
			keyRegs[i] = i;
			valueRegs[i] = i;
		}
		
		for (Entry<Integer, Integer> entry : ints.entrySet()) {
			if (entry.getKey() == null) {
				testFailed("map contains entry with key==null");
			}
			if (entry.getValue() == null) {
				testFailed("map contains entry with value==null");
			}
			int key = entry.getKey();
			int value = entry.getValue();
			
			if (keyRegs[key] == null) {
				testFailed("key " + key + " appears in the map twice");
			}
			keyRegs[key] = null;
			
			if (valueRegs[value] == null) {
				testFailed("value " + value + " appears in the map twice");
			}
			valueRegs[value] = null;			
		}
		
		for (int i = 0; i < COUNT; ++i) {
			if (keyRegs[i] != null) {
				testFailed("key " + i + " is not in the map");
			}
			if (valueRegs[i] != null) {
				testFailed("value " + i + " is not in the map");
			}
		}
		
		for (int i = 0; i < COUNT; ++i) {
			Integer result = ints.get(i);
			if (result == null) {
				testFailed("key " + i + " is not in the map");
			}
			if (result != i) {
				testFailed("value under the key " + i + " is not " + i + " but " + result);
			}
		}
		
		testOk();
	}
	
}
