package cz.cuni.amis.utils;

import org.junit.Test;

import cz.cuni.amis.tests.BaseTest;

public class Test02_SafeEquals extends BaseTest {

	@Test
	public void test01() {
		Integer i1 = 1;
		Integer i2 = 2;
		Integer nullInt = null;
		
		assertTrue("does not hold: 1 != 2", !SafeEquals.equals(i1, i2));
		assertTrue("does not hold: 2 != 1", !SafeEquals.equals(i2, i1));
		assertTrue("does not hold: 1 != nullInt", !SafeEquals.equals(i1, nullInt));
		assertTrue("does not hold: 2 != nullInt", !SafeEquals.equals(i2, nullInt));
		assertTrue("does not hold: nullInt != 2", !SafeEquals.equals(nullInt, i2));
		assertTrue("does not hold: nullInt != 1", !SafeEquals.equals(nullInt, i1));
		assertTrue("does not hold: nullInt == nullInt", SafeEquals.equals(nullInt, nullInt));
		
		testOk();
	}
	
	
}
