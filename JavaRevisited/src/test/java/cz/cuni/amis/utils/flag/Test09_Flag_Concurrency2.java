package cz.cuni.amis.utils.flag;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import cz.cuni.amis.tests.ConcurrencyTest;
import cz.cuni.amis.tests.ConcurrentTask;

public class Test09_Flag_Concurrency2 extends ConcurrencyTest {

	public static final int COUNT = 1000;
	
	private Flag<Integer> flag = new Flag<Integer>(-1);
	
	private class SetFlagJob extends ConcurrentTask {
		
		private FlagListenerMock<Integer> myListener = new FlagListenerMock<Integer>();
		
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
			flag.addWeakListener(myListener);
			for (int i = from; i < to; ++i) {
				flag.setFlag(i);
			}
			flag.removeListener(myListener);			
			myListener.checkValuesSubset("Thread" + threadNum + "-1/4", checkValues);
			
			for (int i = from; i < to; ++i) {
				flag.setFlag(i);
			}			
			myListener.checkValuesInOrder("Thread" + threadNum + "-2/4", new Integer[0]);
			
			flag.addWeakListener(myListener);
			for (int i = from; i < to; ++i) {
				flag.setFlag(i);
			}
			flag.removeListener(myListener);	
			myListener.checkValuesSubset("Thread" + threadNum + "-3/4", checkValues);	
			
			for (int i = from; i < to; ++i) {
				flag.setFlag(i);
			}			
			myListener.checkValuesInOrder("Thread" + threadNum + "-2/4", new Integer[0]);
		}
		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test01() {
		int threadCount = 30;
		
		List<SetFlagJob> tasks = new ArrayList<SetFlagJob>();
		
		for (int i = 0; i < threadCount; ++i) {
			tasks.add(new SetFlagJob(i, i*COUNT, (i+1)*COUNT));
		}
		
		runConcurrent((List<ConcurrentTask>)(List)tasks, threadCount);
		
		testOk();
	}
	
}
