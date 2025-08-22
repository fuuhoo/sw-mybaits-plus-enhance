package cn.siwei.fubin.swmybatisenhance.helper;

import cn.siwei.fubin.swmybatisenhance.annotation.FilterFiled;
import cn.siwei.fubin.swmybatisenhance.constant.FilterTypeEnum;
import cn.siwei.fubin.swmybatisenhance.model.TimeFilterModel;
import cn.siwei.fubin.swmybatisenhance.util.ClassNameConvertUtil;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import static org.springframework.util.ObjectUtils.isEmpty;

@Component
@Slf4j
@ConditionalOnClass(name = "org.springframework.data.mongodb.core.MongoTemplate")
public class MongodbFilterHelper {

    public    Map<String, Object> getFilterField(Object oj)  {
        HashMap<String, Object> filterMap = new HashMap<>();
        HashMap<String, Object> eqMap = new HashMap<>();
        HashMap<String, Object> likeMap = new HashMap<>();
        HashMap<String, Object> leftMap = new HashMap<>();
        HashMap<String, Object> rightMap = new HashMap<>();
        HashMap<String, Object> listMap = new HashMap<>();

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


            if(b) {
                name = ClassNameConvertUtil.toUnderScoreCase(name);
                listField= ClassNameConvertUtil.toUnderScoreCase(listField);
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
                }
            }

        }
        filterMap.put("eq",eqMap);
        filterMap.put("like",likeMap);
        filterMap.put("left",leftMap);
        filterMap.put("right",rightMap);
        filterMap.put("list",listMap);
        return filterMap;
    }

    public Criteria getCriterizByAnnotation(Object oj, TimeFilterModel tfModerl, String  timeFiled) {
        if(timeFiled==null){
            timeFiled  = "updateTime";
        }
        Map<String, Object> filterField = this.getFilterField(oj);
        Criteria criteria = new Criteria();
        Map<String, Object> eqMap = (Map<String, Object>) filterField.get("eq");
        if(!isEmpty(eqMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : eqMap.entrySet()) {
                if(!isEmpty(stringObjectEntry.getValue())) {
                    criteria=criteria.and(stringObjectEntry.getKey()).is(stringObjectEntry.getValue());
                }
            }
        }
        Map<String, Object> likeMap = (Map<String, Object>) filterField.get("like");
        if(!isEmpty(likeMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : likeMap.entrySet()) {
                if(!isEmpty(stringObjectEntry.getValue())) {
                    criteria=criteria.and(stringObjectEntry.getKey()).regex(stringObjectEntry.getValue().toString(),"i");
                }
            }
        }

        Map<String, Object> leftMap = (Map<String, Object>) filterField.get("left");
        if(!isEmpty(leftMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : leftMap.entrySet()) {
                if(!isEmpty(stringObjectEntry.getValue())) {
                    criteria=criteria. and(stringObjectEntry.getKey()).regex("^"+stringObjectEntry.getValue().toString(),"i");
                }
            }
        }

        Map<String, Object> rightqMap = (Map<String, Object>) filterField.get("right");
        if(!isEmpty(rightqMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : rightqMap.entrySet()) {
                if(!isEmpty(stringObjectEntry.getValue())) {
                    criteria=criteria. and(stringObjectEntry.getKey()).regex(stringObjectEntry.getValue().toString()+"$","i");
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
                            criteria= criteria.and(stringObjectEntry.getKey()).in(listValue);
                        }
                    }
                }
            }
        }

        //如果有时间筛选才参数，根据时间进行筛选
        if(!ObjectUtils.isEmpty(tfModerl)){
            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sTime = tfModerl.getSTime();
            String eTime = tfModerl.getETime();
            try {
                Date s = sdf.parse(sTime);
                Date e = sdf.parse(eTime);
                if(!ObjectUtils.isEmpty(sTime)){
                    criteria=criteria.and(timeFiled).gte(s);
                    if(!ObjectUtils.isEmpty(eTime)){
                        criteria=criteria.lte(e);
                    }
                }else{
                    if(!ObjectUtils.isEmpty(eTime)){
                        criteria=criteria.and(timeFiled).lte(e);
                    }
                }
            }catch (Exception e){
                log.error("时间筛选错误:"+e.getMessage());
            }
        }

        return criteria;

    }

    public  Criteria getCriterizByAnnotation(Object oj) {
        Criteria criterizByAnnotation = getCriterizByAnnotation(oj,null,null);
        return criterizByAnnotation;
    }

    public  Criteria getCriterizByAnnotation(Object oj,TimeFilterModel tfModerl) {
        Criteria criterizByAnnotation = getCriterizByAnnotation(oj,tfModerl,null);
        return criterizByAnnotation;
    }

    public Query getMongoQueryByAnnotation(Object oj, TimeFilterModel tfModerl, String  timeFiled) {
        Criteria criterizByAnnotation = getCriterizByAnnotation(oj,tfModerl,timeFiled);
        Query query = new Query(criterizByAnnotation);
        return  query;
    }

    public Query getMongoQueryByAnnotation(Object oj, TimeFilterModel tfModerl) {
        Criteria criterizByAnnotation = getCriterizByAnnotation(oj,tfModerl,null);
        Query query = new Query(criterizByAnnotation);
        return  query;
    }

    public Query getMongoQueryByAnnotation(Object oj) {
        Criteria criterizByAnnotation = getCriterizByAnnotation(oj,null,null);
        Query query = new Query(criterizByAnnotation);
        return  query;
    }
}
