/**
 * 
 */
package com.z.idleaf.support;

import org.junit.Test;

import com.z.idleaf.support.IgniteIdLeafServiceImpl;

/**
 * @author sunff
 *
 */
public class IgniteIdLeafServiceTest {

	@Test
	public void getId() {

		IgniteIdLeafServiceImpl idLeaf = new IgniteIdLeafServiceImpl();
		idLeaf.setBizTag("order");
		idLeaf.setZkAddress("localhost:2181");
		idLeaf.init();
		while (true) {
			System.out.println(idLeaf.getId());
		}
	}
}
