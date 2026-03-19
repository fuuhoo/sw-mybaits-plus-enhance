package cn.siwei.fubin.swmybatisenhance.model;


import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class TimeFilterModel {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 开始时间，不填默认最近100天前
     */
    private String sTime;

    /**
     * 结束时间，不填默认当前时间
     */
    private String eTime;

    public TimeFilterModel() {
        // 设置结束时间为当前时间
        LocalDateTime endTime = LocalDateTime.now();
        this.eTime = endTime.format(FORMATTER);

        // 设置开始时间为100天前
        LocalDateTime startTime = endTime.minusDays(100);
        this.sTime = startTime.format(FORMATTER);
    }

    public String getSTime() {
        return sTime;
    }

    public void setSTime(String sTime) {
        this.sTime = sTime;
    }

    public String getETime() {
        return eTime;
    }

    public void setETime(String eTime) {
        this.eTime = eTime;
    }
}
