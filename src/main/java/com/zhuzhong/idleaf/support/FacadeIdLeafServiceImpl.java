/**
 * 
 */
package com.zhuzhong.idleaf.support;

import java.util.Map;

import com.zhuzhong.idleaf.FacadeIdLeafService;
import com.zhuzhong.idleaf.IdLeafService;

/**
 * @author sunff
 * 
 */
public class FacadeIdLeafServiceImpl implements FacadeIdLeafService {

    private Map<String, IdLeafService> leafServiceMap;

    public FacadeIdLeafServiceImpl(Map<String, IdLeafService> leafServiceMap) {
        this.leafServiceMap = leafServiceMap;
    }

    @Override
    public Long getId(String bizTag) {
        return leafServiceMap.get(bizTag).getId();
    }

}
