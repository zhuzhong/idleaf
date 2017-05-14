package com.zhuzhong.idleaf.support;

public class IdSegment {
    //private  Long minId;
    private  Long maxId;

    private  Long step;

    public IdSegment() {
      
    }

    public Long getMinId() {
        if(this.maxId!=null&&this.step!=null){
            return this.maxId-this.step;
        }else{
            throw new RuntimeException("maxid or step is null");
        }
       // return minId;
    }

  /*  public void setMinId(Long minId) {
        this.minId = minId;
    }*/

    public Long getMaxId() {
        return maxId;
    }

    public void setMaxId(Long maxId) {
        this.maxId = maxId;
    }

    public Long getStep() {
        return step;
    }

    public void setStep(Long step) {
        this.step = step;
    }

   

}