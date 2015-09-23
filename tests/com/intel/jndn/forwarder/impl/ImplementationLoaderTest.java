/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.jndn.forwarder.impl;

import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ImplementationLoaderTest {

	@Before
	public void setup() {
		ImplementationLoader.clear();
	}

	@Test
	public void testLoadNone() {
		List<TestInterface> load = ImplementationLoader.load(TestInterface.class);
		assertEquals(0, load.size());
	}

	@Test
	public void testLoadManually() {
		ImplementationLoader.register(new TestClass());
		List<TestInterface> load = ImplementationLoader.load(TestInterface.class);
		assertEquals(1, load.size());
	}

	public interface TestInterface {

	}

	public class TestClass implements TestInterface {

	}
}
