/**
 * 
 */
package com.zhuzhong.idleaf;

/**
 * @author sunff
 *
 */
public interface FacadeIdLeafService {

    
    /**
     * 根据业务标识查询相应的id
     * @param bizTag
     * @return
     */
    public Long getId(String bizTag);
}
