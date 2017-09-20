/**
 * 
 */
package com.zhuzhong.idleaf.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import com.zhuzhong.idleaf.IdLeafService;
import com.zhuzhong.idleaf.IdLeafServiceFactory;

/**
 * @author sunff
 *
 */
public class DefaultIdLeafServiceFactory implements IdLeafServiceFactory {

	private static ConcurrentHashMap<String, IdLeafService> bizTagIdLeaf = new ConcurrentHashMap<>();

	
	private ExecutorService taskExecutor;
	
	public void setTaskExecutor(ExecutorService taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	public Long getIdByBizTag(String bizTag) {

		IdLeafService issdervice = null;

		if (bizTagIdLeaf.get(bizTag) == null) {
			synchronized (bizTagIdLeaf) {
				if (bizTagIdLeaf.get(bizTag) == null) {
					MysqlIdLeafServiceImpl idleafService = new MysqlIdLeafServiceImpl();
					idleafService.setBizTag(bizTag);
					idleafService.setAsynLoadingSegment(true);
					idleafService.setTaskExecutor(taskExecutor);
					idleafService.init();
					bizTagIdLeaf.putIfAbsent(bizTag, idleafService);

				}
			}
		}

		issdervice = bizTagIdLeaf.get(bizTag);
		return issdervice.getId();
	}

}
