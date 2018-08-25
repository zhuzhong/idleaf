/**
 * 
 */
package com.z.idleaf.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.z.idleaf.IdLeafService;

/**
 * @author sunff
 * 
 */
// @Transactional
// @TransactionConfiguration(transactionManager = "txManager", defaultRollback =
// true)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:com/z/idleaf/support/applicationContext.xml" })
public class MysqlIdleafServiceImplTest {

    @Autowired
    @Qualifier("orderIdLeafService")
    private IdLeafService idLeafService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private BlockingQueue<Long> queue = new LinkedBlockingQueue<Long>(1000);

    @Test
    public void synGetId() {
        while (true)
            System.out.println(idLeafService.getId());
    }

    @Autowired
    private TransactionTemplate transactionTemplate; 
    @Test
    public void batchInsert() {

        List<Long> list = new ArrayList<Long>(1000);
        for (Long i = 0L; i < 1000L; i++) {
            list.add(i);
        }
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            

            final List<Long> insertedList = list;
            
            
            transactionTemplate.execute(new TransactionCallback<Integer>() {

                @Override
                public Integer doInTransaction(TransactionStatus status) {
                    jdbcTemplate.batchUpdate("insert into id_test(p_id) values(?)", new BatchPreparedStatementSetter() {
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            Long insertedId = insertedList.get(i);
                            ps.setLong(1, insertedId);
                        }

                        public int getBatchSize() {
                            return insertedList.size();
                        }
                    });
                    return insertedList.size();
                }
            });
           
        System.out.println("oooolk");
            
         
        } catch (Exception e) {

        }
    }

    @Test
    public void getId() {

        new Thread() {
            public void run() {
                List<Long> list = new ArrayList<Long>(10000);
                while (true) {
                    try {
                        Long id = queue.take();
                        // jdbcTemplate.update("insert into id_test(p_id) values(?)",
                        // l);
                       // System.out.println("id=" + id);
                      
                        if (list.size()== 10000) {

                            final List<Long> insertedList = list;
                            
                            
                            transactionTemplate.execute(new TransactionCallback<Integer>() {

                                @Override
                                public Integer doInTransaction(TransactionStatus status) {
                                    jdbcTemplate.batchUpdate("insert into id_test(p_id) values(?)", new BatchPreparedStatementSetter() {
                                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                                            Long insertedId = insertedList.get(i);
                                            ps.setLong(1, insertedId);
                                        }

                                        public int getBatchSize() {
                                            return insertedList.size();
                                        }
                                    });
                                    return insertedList.size();
                                }
                            });
                            list.clear();

                        } else {
                            list.add(id);
                        }
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
        }.start();

        int count = 0;
        while (true) {
            // System.out.println(idLeafService.getId());
            try {
                queue.put(idLeafService.getId());
                count++;
                if (count % 1000 == 0) {
                    System.out.println("current count no is " + count);
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

       
    }
}
