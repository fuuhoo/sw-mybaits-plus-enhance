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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.util.ObjectUtils.isEmpty;


/**
 * @description: 获取筛选字段的帮助类
 * @author fuhongbin
 * @date 2023/8/18  17:17
 * @param
 * @return
**/


@Component
@Slf4j
public class FilterWrapperHelper<T> {

    private static final ObjectMapper objectMapper = new ObjectMapper();


    public   Map<String, Object> getFilterField(Object oj)  {
        HashMap<String, Object> filterMap = new HashMap<>();
        HashMap<String, Object> eqMap = new HashMap<>();
        HashMap<String, Object> likeMap = new HashMap<>();
        HashMap<String, Object> leftMap = new HashMap<>();
        HashMap<String, Object> rightMap = new HashMap<>();
        HashMap<String, Object> listMap = new HashMap<>();

        HashMap<String, Object> jsonListMap = new HashMap<>();
        HashMap<String, Object> jsonObjectMap = new HashMap<>();
        HashMap<String, Object> jsonObjListectMap = new HashMap<>();



        if(ObjectUtils.isEmpty(oj)){
            return filterMap;
        }

        Field[] declaredFields = oj.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);

            FilterFiled annotation = declaredField.getAnnotation(FilterFiled.class);
            if (annotation==null){
                continue;
            }
            boolean b = annotation.ifCamel2UnderLine();
            String name=declaredField.getName();
            String listField = annotation.listField();
            String aliasName = annotation.aliasName();


            if(b) {

                //字段名称
                name = ClassNameConvertUtil.toUnderScoreCase(name);

                listField= ClassNameConvertUtil.toUnderScoreCase(listField);

                aliasName=ClassNameConvertUtil.toUnderScoreCase(aliasName);
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
                }else if(type.equals(FilterTypeEnum.LIST)){
                    if(!listField.equals("")) {
                        listMap.put(listField, value);
                    }
                }else if(type.equals(FilterTypeEnum.JSON_LIST)){
//                    String key="JSON_OVERLAPS("+name+", {0})";
//                    //如果value是数组，取第一个
//                    if(value instanceof List){
//                        value = ((List<?>) value).stream()
//                                .map(s -> "\"" + s + "\"")
//                                .collect(Collectors.joining(", ", "(", ")"));
//                    }

                    try {
                        String jsonArray = objectMapper.writeValueAsString(value);
                        String format = String.format("JSON_OVERLAPS(%s,  CAST('%s' AS JSON))", name, jsonArray);
                        jsonListMap.put(name,format);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("JSON转换失败", e);
                    }

                }
                else if(type.equals(FilterTypeEnum.JSON_OBJ)){
                    //json obj的字段名,   name 是本字段的字段名
                    String s = annotation.jsonObjName();

                    if(b) {
                        s = ClassNameConvertUtil.toUnderScoreCase(s);
                    }
                    String key=s+"->>'$."+aliasName+"' > JSON_QUOTE({0})";
                    jsonObjectMap.put(key,value);
                }
                else if(type.equals(FilterTypeEnum.JSON_OBJ_LIST)){
                    //json obj的字段名,name 是本字段的字段名
                    String s = annotation.jsonObjName();
                    if(b) {
                        s = ClassNameConvertUtil.toUnderScoreCase(s);
                    }
                    String key="JSON_CONTAINS("+s+", JSON_OBJECT('"+aliasName+"', JSON_QUOTE({0})))";
                    jsonObjectMap.put(key,value);
                }
            }

        }
        filterMap.put("eq",eqMap);
        filterMap.put("like",likeMap);
        filterMap.put("left",leftMap);
        filterMap.put("right",rightMap);
        filterMap.put("list",listMap);
        filterMap.put("jsonList",jsonListMap);
        filterMap.put("jsonObj",jsonObjectMap);

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
    public LambdaQueryWrapper<T>  getLambdaWrapperbyFiledAnnotion(Object oj,TimeFilterModel tfModerl) {

        QueryWrapper<T> tQueryWrapper = new QueryWrapper<T>();

        return getQueryFilterWrapperbyAnotation(tQueryWrapper,oj,tfModerl).lambda();
    }
    public LambdaQueryWrapper<T>  getLambdaWrapperbyFiledAnnotion(Object oj,TimeFilterModel tfModerl,String  timeFiled) {

        QueryWrapper<T> tQueryWrapper = new QueryWrapper<T>();

        return getQueryFilterWrapperbyAnotation(tQueryWrapper,oj,tfModerl,timeFiled).lambda();

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


    public QueryWrapper<T> getWrapperbyFiledAnnotion(Object oj,TimeFilterModel tfModerl, String  timeFiled) {
        QueryWrapper<T> tQueryWrapper = new QueryWrapper<>();
        return getQueryFilterWrapperbyAnotation(tQueryWrapper,oj,tfModerl,timeFiled);
    }

    public QueryWrapper<T> getWrapperbyFiledAnnotion(Object oj,TimeFilterModel tfModerl) {
        QueryWrapper<T> tQueryWrapper = new QueryWrapper<>();
        return getQueryFilterWrapperbyAnotation(tQueryWrapper,oj,tfModerl,null);
    }

    public QueryWrapper<T> getWrapperbyFiledAnnotion(Object oj) {
        QueryWrapper<T> tQueryWrapper = new QueryWrapper<>();
        return getQueryFilterWrapperbyAnotation(tQueryWrapper,oj,null,null);
    }


    public QueryWrapper<T> getQueryFilterWrapperbyAnotation(QueryWrapper<T> qw, Object oj,TimeFilterModel tfModerl){
        return getQueryFilterWrapperbyAnotation(qw,oj,tfModerl,null);
    }

    public QueryWrapper<T> getQueryFilterWrapperbyAnotation(QueryWrapper<T> qw, Object oj){
        return getQueryFilterWrapperbyAnotation(qw,oj,null,null);
    }

    public QueryWrapper<T> getQueryFilterWrapperbyAnotation(QueryWrapper<T> qw, Object oj,TimeFilterModel tfModerl, String  timeFiled) {
        Map<String, Object> filterField = this.getFilterField(oj);
        if (timeFiled==null){
            timeFiled="updateTime";
        }
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

        Map<String, Object> listMap = (Map<String, Object>) filterField.get("list");
        if(!isEmpty(listMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : listMap.entrySet()) {
                if(!isEmpty(stringObjectEntry.getValue())) {
                    //被注解的字段的值
                    Object value = stringObjectEntry.getValue();
                    if(value instanceof List) {
                        List listValue= (List<Object>) value;
                        if(listValue.size()>0) {
                            qw.in(stringObjectEntry.getKey(), listValue);
                        }
                    }
                }
            }
        }



        Map<String, Object> jsonlistMap = (Map<String, Object>) filterField.get("jsonList");
        if(!isEmpty(jsonlistMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : jsonlistMap.entrySet()) {
                if(!isEmpty(stringObjectEntry.getValue())) {
                    //被注解的字段的值
                    String key = stringObjectEntry.getKey();
                    String value = stringObjectEntry.getValue().toString();
                    qw.apply(value);
                }
            }
        }


        Map<String, Object> jsonObjMap = (Map<String, Object>) filterField.get("jsonObj");
        if(!isEmpty(jsonObjMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : jsonObjMap.entrySet()) {
                if(!isEmpty(stringObjectEntry.getValue())) {
                    //被注解的字段的值
                    String key = stringObjectEntry.getKey();

                    Object value = stringObjectEntry.getValue();
                    //key是构造的json查询条件
                    qw.apply(key, value);
                }
            }
        }


        //如果有时间筛选才参数，根据时间进行筛选
        if(!ObjectUtils.isEmpty(tfModerl)){
            String sTime = tfModerl.getSTime();
            String eTime = tfModerl.getETime();
            if(!ObjectUtils.isEmpty(sTime)){
                qw.ge(timeFiled,sTime);
            }
            if(!ObjectUtils.isEmpty(eTime)){
                qw.le(timeFiled,eTime);
            }
//            //增加默认更新时间倒序
//            qw.orderByAsc(timeFiled);
        }
        return qw;
    }



}
