/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.z.idleaf.support;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.z.idleaf.IdLeafService;

/**
 * from
 * https://github.com/twitter/snowflake/blob/master/src/main/scala/com/twitter/service/snowflake/IdWorker.scala
 *
 * @author adyliu (imxylz@gmail.com)
 * @since 1.0
 * 
 *        改进版的snowflake,只使用workId,去掉datacenterId
 */
public class ExtendSnowflakeIdLeafService implements IdLeafService {

    private long workerId;

    private final long idepoch;

    private static final long workerIdBits = 10L;

    private static final long maxWorkerId = -1L ^ (-1L << workerIdBits);

    private static final long sequenceBits = 12L;
    private static final long workerIdShift = sequenceBits;
    // private static final long datacenterIdShift = sequenceBits +
    // workerIdBits;
    private static final long timestampLeftShift = sequenceBits + workerIdBits // +
                                                                               // datacenterIdBits
    ;
    private static final long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long lastTimestamp = -1L;
    private long sequence;

    /**
     * 来自jdbc-sharding中关于idkey的生成 使用本机的ip之和作为workId的初始值
     * 
     * @return
     */
    private static long initWorkId() {
        InetAddress address;
        Long workId=0L;
        try {
            address = InetAddress.getLocalHost();
        } catch (final UnknownHostException e) {
            throw new IllegalStateException("can not get localhost ");
        }
        byte[] ipAddressByteArray = address.getAddress();
        
        for(byte b:ipAddressByteArray) {
            workId+=b<0?b+256:b;
        }
        
        return workId;

    }

    

    public ExtendSnowflakeIdLeafService() {

        this(initWorkId(), 0, 1344322705519L);
    }

    //
    public ExtendSnowflakeIdLeafService(long workerId, long sequence, long idepoch) {
        this.workerId = workerId;

        this.sequence = sequence;
        this.idepoch = idepoch;
        if (workerId < 0 || workerId > maxWorkerId) {
            throw new IllegalArgumentException("workerId is illegal: " + workerId);
        }
        if (idepoch >= System.currentTimeMillis()) {
            throw new IllegalArgumentException("idepoch is illegal: " + idepoch);
        }
    }

    public long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(long workId) {
        this.workerId = workId;
    }

    public long getTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Long getId() {
        long id = nextId();
        return id;
    }

    private synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards.");
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        long id = ((timestamp - idepoch) << timestampLeftShift)//

                | (workerId << workerIdShift)//
                | sequence;
        return id;
    }

    /**
     * get the timestamp (millis second) of id
     * 
     * @param id
     *            the nextId
     * @return the timestamp of id
     */
    public long getIdTimestamp(long id) {
        return idepoch + (id >> timestampLeftShift);
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("IdWorker{");
        sb.append("workerId=").append(workerId);

        sb.append(", idepoch=").append(idepoch);
        sb.append(", lastTimestamp=").append(lastTimestamp);
        sb.append(", sequence=").append(sequence);
        sb.append('}');
        return sb.toString();
    }
}
