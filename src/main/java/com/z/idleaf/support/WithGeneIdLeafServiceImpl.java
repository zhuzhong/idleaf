/**
 * 
 */
package com.z.idleaf.support;

import com.z.idleaf.IdLeafService;
import com.z.idleaf.WithGeneIdLeafService;

/**
 * 带有基因的id生成算法，即在分库分表中会使用
 * 
 * 比如根据user_id进行分库，同一个用户的订单都分到一个库中，不然后续查询会需要遍历所有的分库，
 * 而如果只是单纯的生成全局唯一的order_id,无法满足 单 order_id参数的查询，因为路由规则无法定位相应的分库(如果无法定位相应的分库，只能遍历所有的分库，
 * 这样是不行的)，为了解决这个问题
 * 需要生成的order_id与user_id对于分库路由都可以定位至相应的分库位置
 * 
 * 
 * 思路来自 微信 架构师之路介绍的基因法
 * @author Administrator
 *
 */
public class WithGeneIdLeafServiceImpl implements WithGeneIdLeafService {

	private IdLeafService idLeafService;

	@Override
	public Long getId(Long userId) {
		Long prefixOrderId = idLeafService.getId(); //生成全局唯一的id
		int leftMoveBit = leftMoveBit();
		Long lastOrderId = ((prefixOrderId << leftMoveBit) | (userId % dbSize)); //加入userId基因
		return lastOrderId;  //最终的订单id
	}

	private Integer dbSize;// 分库个数

	public void setDbSize(Integer dbSize) {
		this.dbSize = dbSize;
	}

	private int leftMoveBit() {
		return Integer.toBinaryString(dbSize).length() - 1;
	}

	public static void main(String args[]) {
		WithGeneIdLeafServiceImpl id=new WithGeneIdLeafServiceImpl();
		id.setDbSize(32);
		int dbSize=id.dbSize;
		
		int leftMoveBit = id.leftMoveBit();
		Long userId = 400L;
		Long prefixOrderId = 20000L;
		System.out.println(Integer.toBinaryString(dbSize));
		Long lastOrderId = ((prefixOrderId << leftMoveBit) | (userId % dbSize));
		System.out.println(lastOrderId);

		System.out.println(Integer.toBinaryString(10000));

		System.out.println(Integer.parseInt("1001110001000000000", 2));

		System.out.println(userId % dbSize);

		System.out.println(Integer.toBinaryString((int) (userId % dbSize)));

		System.out.println(Integer.toBinaryString(320016));
	}
}
