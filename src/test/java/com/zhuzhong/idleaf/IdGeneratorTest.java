/**
 * 
 */
package com.zhuzhong.idleaf;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

/**
 * @author sunff
 * 
 */
public class IdGeneratorTest {

    private static LinkedBlockingQueue<Long> q = new LinkedBlockingQueue<Long>(1000);

    @Test
    public void longTest() {
        Long l = new Long(100L);
        long l2 = 100;
        System.out.println(l.equals(l2));
    }

    @Test
    public void getId() {
        final MysqlDemo m = new MysqlDemo();

        System.out.println("Main thread id=" + Thread.currentThread().getId());
        final IdGenerator t = new IdGenerator(m);
        t.init();
        new Thread() {
            public void run() {
                Connection conn = null;
                try {
                    conn = m.getConnection();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                while (true) {
                    try {
                        Long newId = q.take();
                        m.insertIdTest(conn, newId);
                    } catch (InterruptedException e) {
                        e.printStackTrace();

                    }
                }

            }
        }.start();

        new Thread() {
            public void run() {
                while (true) {
                    try {
                        q.put(t.getId());

                    } catch (InterruptedException e) {

                        e.printStackTrace();
                    }
                }

            }
        }.start();

        try {
            System.in.read();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
