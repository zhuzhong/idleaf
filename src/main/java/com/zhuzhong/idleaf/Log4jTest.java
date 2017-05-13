/**
 * 
 */
package com.zhuzhong.idleaf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sunff
 *
 */
public class Log4jTest {

    
    private static Logger logger=LoggerFactory.getLogger(Log4jTest.class);
    
    /**
     * @param args
     */
    public static void main(String[] args) {
       
        logger.debug("this is log4j2 test");
        System.out.println("oooook...");

    }

}
