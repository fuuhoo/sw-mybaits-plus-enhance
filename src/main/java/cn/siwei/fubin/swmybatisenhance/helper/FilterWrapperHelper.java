package cn.siwei.fubin.swmybatisenhance.helper;



import cn.siwei.fubin.swmybatisenhance.annotation.FilterFiled;
import cn.siwei.fubin.swmybatisenhance.constant.FilterTypeEnum;
import cn.siwei.fubin.swmybatisenhance.model.BaseFilterModel;
import cn.siwei.fubin.swmybatisenhance.model.BaseModel;
import cn.siwei.fubin.swmybatisenhance.model.PageFilterModel;
import cn.siwei.fubin.swmybatisenhance.model.TimeFilterModel;
import cn.siwei.fubin.swmybatisenhance.util.ClassNameConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.ObjectUtils.isEmpty;


/**
 * @description: 获取筛选字段的帮助类
 * @author fuhongbin
 * @date 2023/8/18  17:17
 * @param
 * @return
**/


@Component
public class FilterWrapperHelper<T> {


    TimeFilterModel tfModerl;

    public FilterWrapperHelper(TimeFilterModel tfModerl) {
        this.tfModerl = tfModerl;
    }

    public FilterWrapperHelper() {
    }

    private   Map<String, Object> getFilterField(Object oj)  {
        HashMap<String, Object> filterMap = new HashMap<>();
        HashMap<String, Object> eqMap = new HashMap<>();
        HashMap<String, Object> likeMap = new HashMap<>();
        HashMap<String, Object> leftMap = new HashMap<>();
        HashMap<String, Object> rightMap = new HashMap<>();

        if(ObjectUtils.isEmpty(oj)){
            return filterMap;
        }

        Field[] declaredFields = oj.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);
            String name=declaredField.getName();

            name= ClassNameConvertUtil.toUnderScoreCase(name);
            FilterFiled annotation = declaredField.getAnnotation(FilterFiled.class);
            if (annotation==null){
                continue;
            }
            FilterTypeEnum type = annotation.type();
            Object value = null;
            try {
                value = declaredField.get(oj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if(annotation!=null&&value!=null){
                if(type.equals(FilterTypeEnum.EQ)){
                    eqMap.put(name,value);
                }else if(type.equals(FilterTypeEnum.LIKE)){
                    likeMap.put(name,value);
                }
                else if(type.equals(FilterTypeEnum.LEFT)){
                    leftMap.put(name,value);
                }
                else if(type.equals(FilterTypeEnum.RIGHT)){
                    rightMap.put(name,value);
                }
            }

        }
        filterMap.put("eq",eqMap);
        filterMap.put("like",likeMap);
        filterMap.put("left",leftMap);
        filterMap.put("right",rightMap);

        return filterMap;
    }


    public Page<T> getPage(PageFilterModel pfm){

        if(ObjectUtils.isEmpty(pfm)){
            return  new Page<>(1,100);
        }
        Page<T> tPage = new Page<>(pfm.getPageNum(), pfm.getPageSize());
        return tPage;
    }

    public LambdaQueryWrapper<T>  getLambdaWrapperbyFiledAnnotion(Object oj) {

        QueryWrapper<T> tQueryWrapper = new QueryWrapper<T>();

        return getQueryFilterWrapperbyAnotation(tQueryWrapper,oj).lambda();
    }

    public  void getBaseFilter(LambdaQueryWrapper< ? extends BaseModel> tQueryWrapper, BaseFilterModel baseFilterModel){



        String creatUser = baseFilterModel.getCreatUser();
        if(!ObjectUtils.isEmpty(creatUser)) {
            tQueryWrapper.eq(BaseModel::getCreateUser, creatUser);
        }

        String updateUser = baseFilterModel.getUpdateUser();
        if(!ObjectUtils.isEmpty(updateUser)) {
            tQueryWrapper.eq(BaseModel::getUpdateUser, updateUser);
        }

        Date creatTimeStart = baseFilterModel.getCreatTimeStart();
        if(!ObjectUtils.isEmpty(creatTimeStart)) {
            tQueryWrapper.ge(BaseModel::getCreateTime, creatTimeStart);
        }

        Date creatTimeEnd = baseFilterModel.getCreatTimeEnd();
        if(!ObjectUtils.isEmpty(creatTimeEnd)) {
            tQueryWrapper.le(BaseModel::getCreateTime, creatTimeEnd);
        }

        Date updateTimeStart = baseFilterModel.getUpdateTimeStart();
        if(!ObjectUtils.isEmpty(updateTimeStart)) {
            tQueryWrapper.ge(BaseModel::getUpdateTime, updateTimeStart);
        }

        Date updateTimeEnd = baseFilterModel.getUpdateTimeEnd();
        if(!ObjectUtils.isEmpty(updateTimeEnd)) {
            tQueryWrapper.ge(BaseModel::getUpdateTime, updateTimeEnd);
        }






    }

//    //开启数据权限
//    public void enableDataPermission() {
//
//    }


    public QueryWrapper<T> getWrapperbyFiledAnnotion(Object oj) {

        QueryWrapper<T> tQueryWrapper = new QueryWrapper<>();

        return getQueryFilterWrapperbyAnotation(tQueryWrapper,oj);

    }

    public QueryWrapper<T> getQueryFilterWrapperbyAnotation(QueryWrapper<T> qw, Object oj) {


        Map<String, Object> filterField = this.getFilterField(oj);

        Map<String, Object> eqMap = (Map<String, Object>) filterField.get("eq");
        if(!isEmpty(eqMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : eqMap.entrySet()) {
                if(!isEmpty(stringObjectEntry.getValue())) {
                    qw.eq(stringObjectEntry.getKey(), stringObjectEntry.getValue());
                }
            }
        }
        Map<String, Object> likeMap = (Map<String, Object>) filterField.get("like");
        if(!isEmpty(likeMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : likeMap.entrySet()) {
                if(!isEmpty(stringObjectEntry.getValue())) {
                    qw.like(stringObjectEntry.getKey(), stringObjectEntry.getValue());
                }
            }
        }
        Map<String, Object> leftMap = (Map<String, Object>) filterField.get("left");
        if(!isEmpty(leftMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : leftMap.entrySet()) {
                if(!isEmpty(stringObjectEntry.getValue())) {

                    qw.likeLeft(stringObjectEntry.getKey(), stringObjectEntry.getValue());
                }
            }
        }
        Map<String, Object> rightMap = (Map<String, Object>) filterField.get("right");
        if(!isEmpty(rightMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : rightMap.entrySet()) {
                if(!isEmpty(stringObjectEntry.getValue())) {
                    qw.likeRight(stringObjectEntry.getKey(), stringObjectEntry.getValue());
                }
            }
        }

        //如果有时间筛选才参数，根据时间进行筛选
        if(!ObjectUtils.isEmpty(tfModerl)){

            String sTime = tfModerl.getSTime();
            String eTime = tfModerl.getETime();

            if(ObjectUtils.isEmpty(sTime)){
                qw.gt("updateTime",sTime);
            }
            if(ObjectUtils.isEmpty(eTime)){
                qw.lt("updateTime",eTime);
            }
        }

        return qw;
    }


}
