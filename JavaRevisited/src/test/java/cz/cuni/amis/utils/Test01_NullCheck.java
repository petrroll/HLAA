package cz.cuni.amis.utils;

import org.junit.Test;

import cz.cuni.amis.tests.BaseTest;

public class Test01_NullCheck extends BaseTest {

	@Test
	public void test01() {
		NullCheck.check((Integer)1);
		testOk();
	}
	
	@Test
	public void test02() {
		NullCheck.check("Ahoj");
		testOk();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void test03() {
		NullCheck.check(null);
		assertFail("Should not reach here!");
	}
	
}
