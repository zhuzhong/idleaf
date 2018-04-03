# idleaf

## 简介


对于 [Leaf——美团点评分布式ID生成系统](http://tech.meituan.com/MT_Leaf.html?utm_source=tuicool&utm_medium=referral "Leaf——美团点评分布式ID生成系统") 中介绍的
Leaf-segment数据库方案 生成唯一orderId的方案的一个实现。
在实现中使用双buffer优化，在第一个buffer使用50%的时候去加载另一个buffer的数据，这里分同步与异步两种方式，默认是同步加载。对于异步增加参数asynLoadingSegment 设为true.
在第一个buffer使用完毕之后，切换到另一个buffer，需要去验证该buffer是否加载完成数据，然后进行切换（对于异步加载出了异常则同步加载数据，然后再切换，此时会产生发号的阻塞）。




## 使用示例
### 一种业务id使用方式

	<bean id="idLeafService" 	class="com.z.idleaf.support.MysqlIdLeafServiceImpl">
		<property name="jdbcTemplate" ref="jdbcTemplate" />
		<property name="asynLoadingSegment" value="true" />
		<property name="bizTag" value="order"></property>
	</bean>

	Long id=idLeafService.getId()
### 多个业务id使用方式

    <bean id="parentIdLeafService"  abstract="true" 	class="com.z.idleaf.support.MysqlIdLeafServiceImpl">
		<property name="jdbcTemplate" ref="jdbcTemplate" />
		<property name="asynLoadingSegment" value="true" />
		
	</bean>

	<bean id="productIdLeafService"  parent="parentIdLeafService">
		<property name="bizTag" value="product"></property>
	</bean>

	<bean id="orderIdLeafService"  parent="parentIdLeafService">
		<property name="bizTag" value="order"></property>
	</bean>

	


    Long orderId=orderIdLeafService.getId();
	Long productId=productIdLeafService.getId();

## spring提供的关于主键生成的策略

spring 框架是无所不能，关于主键的生成它也提供了类似的功能，相应的类为org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer
但是这个类只保证单jvm的唯一性，在集群环中它会有并发更新的问题，所以我在此写了一个ExtendMySQLMaxValueIncrementer。思路还是沿袭上面的。
### 使用示例
	<bean id="extendMysqlMaxValueIncrementer" abstract="true"
		class="com.z.idleaf.support.ExtendMySQLMaxValueIncrementer">
		<property name="dataSource" ref="testDataSource" />
		<property name="asynLoadingSegment" value="true"></property>
		<property name="incrementerName" value="id_segment" />
		<property name="columnName" value="max_id" />
		<property name="stepField" value="p_step" />
		<property name="bizField" value="biz_tag"></property>
		<property name="lastUpdateTimeField" value="last_update_time" />
		<property name="updateTimeField" value="current_update_time" />
		<property name="paddingLength" value="6"></property>

	</bean>

	<bean id="orderIncrementer" parent="extendMysqlMaxValueIncrementer">
		<property name="bizTag" value="order" />
	</bean>
	
	<bean id="productNoIncrementer" parent="extendMysqlMaxValueIncrementer">
		<property name="bizTag" value="productNo" />
	</bean>

	 @Autowired
    @Qualifier("productNoIncrementer")
    private DataFieldMaxValueIncrementer incrementer;

    @Test
    public void test() {
        int i = 0;
        while (i < 10) {
            System.out.println("long id=" + incrementer.nextLongValue());
            System.out.println("int id=" + incrementer.nextIntValue());
            System.out.println("string id=" + incrementer.nextStringValue());
            i++;
        }
    }

spring 帮我们提供了三个接口，分别为获取int,long,string 三种数据类型。当然这种方式需要深度使用spring-jdbc框架.




## 带基因法id生成


基因法生成id,主要思路来自[架构师之路](http://mp.weixin.qq.com/s/PCzRAZa9n4aJwHOX-kAhtA)
实现接口com.z.idleaf.WithGeneIdLeafService
使用示例
	

	<bean id="idLeafService"    class="com.z.idleaf.support.MysqlIdLeafServiceImpl">
	    <property name="jdbcTemplate" ref="jdbcTemplate" />
	    <property name="asynLoadingSegment" value="true" />
	    <property name="bizTag" value="order"></property>
	</bean>
	<bean id="withGeneIdLeafService" class="com.z.idleaf.support.WithGeneIdLeafServiceImpl">
		 <property name="idLeafService" ref="idLeafService" />
	    <property name="dbSize" value="32" />
	</bean>
	Long orderId=withGeneIdLeafService.getId(10000L)
