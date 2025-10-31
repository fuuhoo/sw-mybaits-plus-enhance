package cn.siwei.fubin.swmybatisenhance.model;


import lombok.Data;

@Data
public class TimeFilterModel {

    /**
     * 开始时间，不填默认2000-01-01 00:00:00
    */
    String sTime="2000-01-01 00:00:00";

    /**
     * 结束时间,不填默认：2099-01-01 00:00:00
    */
    String eTime="2099-01-01 00:00:00";

}
