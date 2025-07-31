package cn.siwei.fubin.swmybatisenhance.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class BaseFilterModel {
    String creatUser;
    String updateUser;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:SS",timezone = "GMT+8")
    Date creatTimeStart;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:SS",timezone = "GMT+8")
    Date creatTimeEnd;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:SS",timezone = "GMT+8")
    Date updateTimeStart;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:SS",timezone = "GMT+8")
    Date updateTimeEnd;

    public String getCreatUser() {
        return creatUser;
    }

    public void setCreatUser(String creatUser) {
        this.creatUser = creatUser;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
    }

    public Date getCreatTimeStart() {
        return creatTimeStart;
    }

    public void setCreatTimeStart(Date creatTimeStart) {
        this.creatTimeStart = creatTimeStart;
    }

    public Date getCreatTimeEnd() {
        return creatTimeEnd;
    }

    public void setCreatTimeEnd(Date creatTimeEnd) {
        this.creatTimeEnd = creatTimeEnd;
    }

    public Date getUpdateTimeStart() {
        return updateTimeStart;
    }

    public void setUpdateTimeStart(Date updateTimeStart) {
        this.updateTimeStart = updateTimeStart;
    }

    public Date getUpdateTimeEnd() {
        return updateTimeEnd;
    }

    public void setUpdateTimeEnd(Date updateTimeEnd) {
        this.updateTimeEnd = updateTimeEnd;
    }
}
