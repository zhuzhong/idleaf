package com.zhuzhong.idleaf;

public class Idsegment {
    private final Long minId;
    private final Long maxId;

    private final Long step;

    public Idsegment(Long step, Long maxId) {
        super();
        this.step = step;

        this.maxId = maxId;
        this.minId = maxId - step;
    }

    public Long getMinId() {
        return minId;
    }

    public Long getMaxId() {
        return maxId;
    }

}