/**
 * 
 */
package com.zhuzhong.idleaf;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author sunff
 * 
 */
public class IdGenerator {

    private MysqlDemo m;

    public IdGenerator(MysqlDemo m) {
        this.m = m;
        this.init();
    }

    private volatile Idsegment[] segment = new Idsegment[2]; // 这两段用来存储每次拉升之后的最大值
    private volatile boolean sw;
    private AtomicLong currentId;
    private static ReentrantLock lock = new ReentrantLock();
    private static Condition swContion = lock.newCondition();

    public void init() {
        segment[0] = m.getNextIdsegment();
        segment[1] = m.getNextIdsegment();
        setSw(false);
        currentId = new AtomicLong(segment[index()].getMinId()); // 初始id

    }

    private Long asynGetId() {
        return null;
    }

    private Long synGetId() {
        if (segment[index()].getMaxId().equals(currentId.longValue())) {
            try {
                lock.lock();
                if (segment[index()].getMaxId().equals(currentId.longValue())) {

                    final int currentIndex = index();
                    segment[currentIndex] = m.getNextIdsegment();

                    setSw(!isSw()); // 切换
                    currentId = new AtomicLong(segment[index()].getMinId()); // 进行切换

                }

            } finally {
                lock.unlock();
            }
        }

        return currentId.incrementAndGet();
    }

    public Long getId() {
        return synGetId();
    }

    private boolean isSw() {
        return sw;
    }

    private void setSw(boolean sw) {
        this.sw = sw;
    }

    private int index() {
        if (isSw()) {
            return 1;
        } else {
            return 0;
        }
    }

}
