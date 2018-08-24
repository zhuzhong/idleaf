/**
 * 
 */
package com.z.idleaf.support;

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

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.support.incrementer.AbstractColumnMaxValueIncrementer;

/**
 * @author sunff
 * 
 */
@Deprecated
public class ExtendMySQLMaxValueIncrementer extends AbstractColumnMaxValueIncrementer {

    /**
     * Default constructor for bean property style usage.
     * 
     * @see #setDataSource
     * @see #setIncrementerName
     * @see #setColumnName
     */
    public ExtendMySQLMaxValueIncrementer() {
    }

    /**
     * Convenience constructor.
     * 
     * @param dataSource
     *            the DataSource to use
     * @param incrementerName
     *            the name of the sequence/table to use
     * @param columnName
     *            the name of the column in the sequence table to use
     */
    public ExtendMySQLMaxValueIncrementer(DataSource dataSource, String incrementerName, String columnName) {
        super(dataSource, incrementerName, columnName);
    }

    @Override
    protected long getNextKey() throws DataAccessException {
        if (asynLoadingSegment) {
            return asynGetId();
        } else {
            return synGetId();
        }
    }

    // ------------------

    // 创建线程池
    private static ExecutorService es = Executors.newSingleThreadExecutor();
    private FutureTask<Boolean> asynLoadSegmentTask = null;

    private long asynGetId() {

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
                            segment[currentIndex] = doUpdateNextSegment();
                            return true;
                        }

                    });
                    es.submit(asynLoadSegmentTask);
                }
                if (segment[index()].getMaxId().equals(currentId.longValue())) {

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
                        segment[currentIndex] = doUpdateNextSegment();
                        setSw(!isSw()); // 切换
                        currentId = new AtomicLong(segment[index()].getMinId()); // 进行切换
                    } catch (ExecutionException e) {

                        e.printStackTrace();
                        // 强制同步切换　
                        final int currentIndex = reIndex();
                        segment[currentIndex] = doUpdateNextSegment();
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

    private long synGetId() {
        if (segment[index()].getMiddleId().equals(currentId.longValue())
                || segment[index()].getMaxId().equals(currentId.longValue())) {
            try {
                lock.lock();

                if (segment[index()].getMiddleId().equals(currentId.longValue())) {
                    // 使用50%进行加载
                    final int currentIndex = reIndex();
                    segment[currentIndex] = doUpdateNextSegment();
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

    private IdSegment doUpdateNextSegment() {
        try {
            return updateId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private IdSegment updateId() throws Exception {

     // String querySql =
        // "select p_step ,max_id ,last_update_time,current_update_time from id_segment where biz_tag=?";

        String querySql = String.format("select %s as  p_step , %s as  max_id,%s as last_update_time ,"
                + " %s as current_update_time   from %s where %s=?", stepField, getColumnName(),
                this.lastUpdateTimeField, this.updateTimeField, getIncrementerName(), this.bizField);

        
        // String updateSql =
        // "update id_segment set max_id=?,last_update_time=?,current_update_time=now() where biz_tag=? and max_id=?";

        String updateSql = String.format("update %s set %s=?  ,%s=?,%s=?  where %s=? and %s=?",
                getIncrementerName(), getColumnName(), this.lastUpdateTimeField, this.updateTimeField, this.bizField,
                getColumnName());

        final IdSegment currentSegment = new IdSegment();
        this.jdbcTemplate.query(querySql, new String[] { bizTag }, new RowCallbackHandler() {

            @Override
            public void processRow(ResultSet rs) throws SQLException {

                Long step = null;
                Long currentMaxId = null;
                step = rs.getLong("p_step");
                currentMaxId = rs.getLong("max_id");
                Date lastUpdateTime = new Date();
                if( rs.getTimestamp("last_update_time")!=null){
                    lastUpdateTime = (java.util.Date) rs.getTimestamp("last_update_time");
                }
                
                Date currentUpdateTime=new Date();
                if( rs.getTimestamp("current_update_time")!=null){
                     currentUpdateTime = (java.util.Date) rs.getTimestamp("current_update_time");    
                }
                currentSegment.setStep(step);
                currentSegment.setMaxId(currentMaxId);
                currentSegment.setLastUpdateTime(lastUpdateTime);
                currentSegment.setCurrentUpdateTime(currentUpdateTime);

            }
        });
        Long newMaxId = currentSegment.getMaxId() + currentSegment.getStep();
        int row = this.jdbcTemplate.update(updateSql, new Object[] { newMaxId, currentSegment.getCurrentUpdateTime(),
                new Date(), bizTag, currentSegment.getMaxId() });
        if (row == 1) {
            IdSegment newSegment = new IdSegment();
            newSegment.setStep(currentSegment.getStep());
            newSegment.setMaxId(newMaxId);

            return newSegment;
        } else {
            return updateId(); // 递归，直至更新成功
        }

    }

    private JdbcTemplate jdbcTemplate;

    private String bizTag;

    private String stepField;
    private String bizField;

    public void setBizField(String bizField) {
        this.bizField = bizField;
    }

    private String lastUpdateTimeField;
    private String updateTimeField;

    public void setLastUpdateTimeField(String lastUpdateTimeField) {
        this.lastUpdateTimeField = lastUpdateTimeField;
    }

    public void setUpdateTimeField(String updateTimeField) {
        this.updateTimeField = updateTimeField;
    }

    public void setStepField(String stepField) {
        this.stepField = stepField;
    }

    public void setBizTag(String bizTag) {
        this.bizTag = bizTag;
    }

    private boolean asynLoadingSegment;

    public void setAsynLoadingSegment(boolean asynLoadingSegment) {
        this.asynLoadingSegment = asynLoadingSegment;
    }

    private volatile IdSegment[] segment = new IdSegment[2]; // 这两段用来存储每次拉升之后的最大值
    private volatile boolean sw;
    private AtomicLong currentId;
    private static ReentrantLock lock = new ReentrantLock();

    private void init() {
        segment[0] = doUpdateNextSegment();

        setSw(false);
        currentId = new AtomicLong(segment[index()].getMinId()); // 初始id

    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
        if (this.bizTag == null) {
            throw new RuntimeException("bizTag must be not null");
        }

        if (this.jdbcTemplate == null) {
            this.jdbcTemplate = new JdbcTemplate(getDataSource());
        }
        this.init();
        log.info("init run success...");
    }

    private static Logger log = LoggerFactory.getLogger(ExtendMySQLMaxValueIncrementer.class);

}
