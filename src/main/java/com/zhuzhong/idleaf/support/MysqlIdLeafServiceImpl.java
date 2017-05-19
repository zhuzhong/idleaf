/**
 * 
 */
package com.zhuzhong.idleaf.support;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.zhuzhong.idleaf.IdLeafService;

/**
 * @author sunff
 * 
 */
public class MysqlIdLeafServiceImpl implements IdLeafService, InitializingBean {

    private static Logger log = LoggerFactory.getLogger(MysqlIdLeafServiceImpl.class);

    public void afterPropertiesSet() throws Exception {
        if (this.bizTag == null) {
            throw new RuntimeException("bizTag must be not null");
        }
        if (this.jdbcTemplate == null) {
            throw new RuntimeException("jdbcTemplate must be not null");
        }
        this.init();
        log.info("init run success...");
    }

    public MysqlIdLeafServiceImpl() {

    }

    private volatile IdSegment[] segment = new IdSegment[2]; // 这两段用来存储每次拉升之后的最大值
    private volatile boolean sw;
    private AtomicLong currentId;
    private static ReentrantLock lock = new ReentrantLock();

    // private static Condition swContion = lock.newCondition();

    private void init() {
        segment[0] = doUpdateNextSegment(bizTag);
        // segment[1] = doUpdateNextSegment(bizTag);
        setSw(false);
        currentId = new AtomicLong(segment[index()].getMinId()); // 初始id

    }

    // 创建线程池
    ExecutorService es = Executors.newSingleThreadExecutor();
    FutureTask<Boolean> asynLoadSegmentTask = null;

    private Long asynGetId() {

        if (segment[index()].getMiddleId().equals(currentId.longValue())
                || segment[index()].getMaxId().equals(currentId.longValue())) {
            try {
                lock.lock();
                if (segment[index()].getMiddleId().equals(currentId.longValue())) {
                    // 前一段使用了50%

                    asynLoadSegmentTask = new FutureTask<>(new Callable<Boolean>() {

                        @Override
                        public Boolean call() throws Exception {
                            final int currentIndex = reIndex();
                            segment[currentIndex] = doUpdateNextSegment(bizTag);
                            return true;
                        }

                    });
                    es.submit(asynLoadSegmentTask);
                }
                if (segment[index()].getMaxId().equals(currentId.longValue())) {

                    /*
                     * final int currentIndex = index(); segment[currentIndex] =
                     * doUpdateNextSegment(bizTag);
                     */
                    boolean loadingResult;
                    try {
                        loadingResult = asynLoadSegmentTask.get();
                        if (loadingResult) {
                            setSw(!isSw()); // 切换
                            currentId = new AtomicLong(segment[index()].getMinId()); // 进行切换
                        }
                    } catch (InterruptedException e) {

                        e.printStackTrace();
                        // 强制同步切换　
                        final int currentIndex = reIndex();
                        segment[currentIndex] = doUpdateNextSegment(bizTag);
                        setSw(!isSw()); // 切换
                        currentId = new AtomicLong(segment[index()].getMinId()); // 进行切换
                    } catch (ExecutionException e) {

                        e.printStackTrace();
                        // 强制同步切换　
                        final int currentIndex = reIndex();
                        segment[currentIndex] = doUpdateNextSegment(bizTag);
                        setSw(!isSw()); // 切换
                        currentId = new AtomicLong(segment[index()].getMinId()); // 进行切换
                    }

                }

            } finally {
                lock.unlock();
            }
        }

        return currentId.incrementAndGet();

    }

    private int reIndex() {
        if (isSw()) {
            return 0;
        } else {
            return 1;
        }
    }

    private Long synGetId() {
        if (segment[index()].getMiddleId().equals(currentId.longValue())
                || segment[index()].getMaxId().equals(currentId.longValue())) {
            try {
                lock.lock();

                if (segment[index()].getMiddleId().equals(currentId.longValue())) {
                    // 使用50%进行加载
                    final int currentIndex = reIndex();
                    segment[currentIndex] = doUpdateNextSegment(bizTag);
                }

                if (segment[index()].getMaxId().equals(currentId.longValue())) {
                    setSw(!isSw()); // 切换
                    currentId = new AtomicLong(segment[index()].getMinId()); // 进行切换

                }

            } finally {
                lock.unlock();
            }
        }

        return currentId.incrementAndGet();
    }

    @Override
    public Long getId() {
        if (asynLoadingSegment) {
            return asynGetId();
        } else {
            return synGetId();
        }
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

    private IdSegment doUpdateNextSegment(String bizTag) {
        try {
            return updateId(bizTag);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private IdSegment updateId(String bizTag) throws Exception {
        /*
         * String querySql
         * =String.format("select %s as  p_step , %s as  max_id  from %s where %s=?"
         * , stepField, maxIdField,tableName,this.bizTagField);
         */
        String querySql = "select p_step ,max_id ,last_update_time,current_update_time from id_segment where biz_tag=?";
        String updateSql = "update id_segment set max_id=?,last_update_time=?,current_update_time=now() where biz_tag=? and max_id=?";
        /*
         * String
         * updateSql=String.format("update %s set %s=? where %s=? and %s=?",
         * tableName,maxIdField,bizTagField,maxIdField);
         */
        final IdSegment currentSegment = new IdSegment();
        this.jdbcTemplate.query(querySql, new String[] { bizTag }, new RowCallbackHandler() {

            @Override
            public void processRow(ResultSet rs) throws SQLException {

                Long step = null;
                Long currentMaxId = null;
                step = rs.getLong("p_step");
                currentMaxId = rs.getLong("max_id");
                Date lastUpdateTime = (java.util.Date) rs.getTimestamp("last_update_time");
                Date currentUpdateTime = (java.util.Date) rs.getTimestamp("current_update_time");
                currentSegment.setStep(step);
                currentSegment.setMaxId(currentMaxId);
                currentSegment.setLastUpdateTime(lastUpdateTime);
                currentSegment.setCurrentUpdateTime(currentUpdateTime);

            }
        });
        Long newMaxId = currentSegment.getMaxId() + currentSegment.getStep();
        int row = this.jdbcTemplate.update(updateSql, 
                new Object[] { newMaxId, currentSegment.getCurrentUpdateTime(),bizTag, currentSegment.getMaxId() });
        if (row == 1) {
            IdSegment newSegment = new IdSegment();
            newSegment.setStep(currentSegment.getStep());
            newSegment.setMaxId(newMaxId);

            return newSegment;
        } else {
            return updateId(bizTag); // 递归，直至更新成功
        }

    }

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private String bizTag;

    public void setBizTag(String bizTag) {
        this.bizTag = bizTag;
    }

    private boolean asynLoadingSegment;

    public void setAsynLoadingSegment(boolean asynLoadingSegment) {
        this.asynLoadingSegment = asynLoadingSegment;
    }

    /*
     * private String stepField; private String maxIdField; private String
     * tableName;
     * 
     * private String bizTagField;
     */

    /*
     * public void setStepField(String stepField) { this.stepField = stepField;
     * }
     * 
     * public void setMaxIdField(String maxIdField) { this.maxIdField =
     * maxIdField; }
     * 
     * public void setTableName(String tableName) { this.tableName = tableName;
     * }
     * 
     * public void setBizTagField(String bizTagField) { this.bizTagField =
     * bizTagField; }
     */

}
