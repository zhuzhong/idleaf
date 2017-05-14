/**
 * 
 */
package com.zhuzhong.idleaf.support;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.zhuzhong.idleaf.IdLeafService;

/**
 * @author sunff
 * 
 */
// @Transactional
// @TransactionConfiguration(transactionManager = "txManager", defaultRollback =
// true)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:com/zhuzhong/idleaf/support/applicationContext.xml" })
public class MysqlIdleafServiceImplTest {

    @Autowired
    private IdLeafService idLeafService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private BlockingQueue<Long> queue = new LinkedBlockingQueue<Long>(1000);

    @Test
    public void getId() {

        new Thread() {
            public void run() {
                while (true) {
                    try {
                        Long l = queue.take();
                        jdbcTemplate.update("insert into id_test(p_id) values(?)", l);

                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
        }.start();
        new Thread(){
            public void run(){
                while (true) {
                    // System.out.println(idLeafService.getId());
                    try {
                        queue.put(idLeafService.getId());
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        System.out.println("ooook");
    }
}
