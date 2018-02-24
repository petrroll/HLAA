package cz.cuni.amis.utils.flag;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import cz.cuni.amis.tests.ConcurrencyTest;
import cz.cuni.amis.tests.ConcurrentTask;

public class Test08_Flag_Concurrency1 extends ConcurrencyTest {

	public static final int COUNT = 1000;
	
	private Flag<Integer> flag = new Flag<Integer>(-1);
	
	private class SetFlagJob extends ConcurrentTask {
		
		private FlagListenerMock<Integer> myListener = new FlagListenerMock<Integer>();
		
		private int from;

		private int to;
		
		public SetFlagJob(int from, int to) {
			flag.addWeakListener(myListener);
			this.from = from;
			this.to = to;
		}

		@Override
		protected void runImpl() {
			for (int i = from; i < to; ++i) {
				flag.setFlag(i);
			}			
		}
		
		public void checkListener(String threadName, Integer[] values) {
			myListener.checkValuesAnyOrder(threadName, values);
		}
		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test01() {
		int threadCount = 30;
		
		List<SetFlagJob> tasks = new ArrayList<SetFlagJob>();
		
		for (int i = 0; i < threadCount; ++i) {
			tasks.add(new SetFlagJob(i*COUNT, (i+1)*COUNT));
		}
		
		runConcurrent((List<ConcurrentTask>)(List)tasks, threadCount);
		
		log.info("Checking results...");
		
		Integer[] checkValues = new Integer[threadCount * COUNT];
		for (int i = 0; i < threadCount * COUNT; ++i) {
			checkValues[i] = i;
		}
		
		int threadNum = 0;
		for (SetFlagJob task : tasks) {
			task.checkListener("Thread" + threadNum, checkValues);
			++threadNum;
		}
		
		testOk();
	}
	
}
