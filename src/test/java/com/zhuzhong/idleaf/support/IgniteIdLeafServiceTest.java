/**
 * 
 */
package com.zhuzhong.idleaf.support;

import com.zhuzhong.idleaf.IdLeafService;

/**
 * @author sunff
 *
 */
public class IgniteIdLeafServiceTest {

	
	
	public void getId() {
		
		IdLeafService idLeaf=new IgniteIdLeafServiceImpl();
		while(true) {
			System.out.println(idLeaf.getId());
		}
	}
}
