#idleaf

##简介


对于 [http://tech.meituan.com/MT_Leaf.html?utm_source=tuicool&utm_medium=referral](http://tech.meituan.com/MT_Leaf.html?utm_source=tuicool&utm_medium=referral "Leaf——美团点评分布式ID生成系统") Leaf——美团点评分布式ID生成系统 中介绍的
Leaf-segment数据库方案 生成唯一orderId的方案的一个实现。
在实现中使用双buffer优化，在第一个buffer使用50%的时候去加载另一个buffer的数据，这里分同步与异步两种方式，默认是同步加载。对于异步增加参数asynLoadingSegment 设为true.
在第一个buffer使用完毕之后，切换到另一个buffer，需要去验证该buffer是否加载完成数据，然后进行切换（对于异步加载出了异常则同步加载数据，然后再切换，此时会产生发号的阻塞）。


##增加facade类
增加FacaIdLeafService类支持更多的业务分类id获取

##使用示例
###一种业务id使用方式

	<bean id="idLeafService" 	class="com.zhuzhong.idleaf.support.MysqlIdLeafServiceImpl">
		<property name="jdbcTemplate" ref="jdbcTemplate" />
		<property name="asynLoadingSegment" value="true" />
		<property name="bizTag" value="order"></property>
	</bean>

	Long id=idLeafService.getId()
###多个业务id使用方式

    <bean id="orderIdLeafService" 	class="com.zhuzhong.idleaf.support.MysqlIdLeafServiceImpl">
		<property name="jdbcTemplate" ref="jdbcTemplate" />
		<property name="asynLoadingSegment" value="true" />
		<property name="bizTag" value="order"></property>
	</bean>

	<bean id="productIdLeafService" 	class="com.zhuzhong.idleaf.support.MysqlIdLeafServiceImpl">
		<property name="jdbcTemplate" ref="jdbcTemplate" />
		<property name="asynLoadingSegment" value="true" />
		<property name="bizTag" value="product"></property>
	</bean>

	<bean id="idLeafService" class="com.zhuzhong.idleaf.support.FacadeIdLeafServiceImpl">
		<constructor-arg>
			<map>
				<entry key="order" value-ref="orderIdLeafService" />
				<entry key="product" value-ref="productIdLeafService" />
			</map>
		</constructor-arg>
	</bean>


    Long orderId=idLeafService.getId("order");
	Long productId=idLeafService.getId("product");