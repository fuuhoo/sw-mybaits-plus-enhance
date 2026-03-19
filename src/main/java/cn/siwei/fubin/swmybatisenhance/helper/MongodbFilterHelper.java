package cn.siwei.fubin.swmybatisenhance.helper;

import cn.siwei.fubin.swmybatisenhance.annotation.FilterFiled;
import cn.siwei.fubin.swmybatisenhance.constant.FilterTypeEnum;
import cn.siwei.fubin.swmybatisenhance.model.PageData;
import cn.siwei.fubin.swmybatisenhance.model.PageFilterModel;
import cn.siwei.fubin.swmybatisenhance.model.TimeFilterModel;
import cn.siwei.fubin.swmybatisenhance.util.ClassNameConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import static org.springframework.util.ObjectUtils.isEmpty;

@Component
@Slf4j
@ConditionalOnClass(name = "org.springframework.data.mongodb.core.MongoTemplate")
public class MongodbFilterHelper<T> {

    public Map<String, Object> getFilterField(Object oj) {
        HashMap<String, Object> filterMap = new HashMap<>();
        HashMap<String, Object> eqMap = new HashMap<>();
        HashMap<String, Object> likeMap = new HashMap<>();
        HashMap<String, Object> leftMap = new HashMap<>();
        HashMap<String, Object> rightMap = new HashMap<>();
        HashMap<String, Object> listMap = new HashMap<>();

        if (ObjectUtils.isEmpty(oj)) {
            return filterMap;
        }

        Field[] declaredFields = oj.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            declaredField.setAccessible(true);

            FilterFiled annotation = declaredField.getAnnotation(FilterFiled.class);
            if (annotation == null) {
                continue;
            }
            boolean b = annotation.ifCamel2UnderLine();
            String name = declaredField.getName();
            String listField = annotation.listField();


            if (b) {
                name = ClassNameConvertUtil.toUnderScoreCase(name);
                listField = ClassNameConvertUtil.toUnderScoreCase(listField);
            }

            FilterTypeEnum type = annotation.type();
            Object value = null;
            try {
                value = declaredField.get(oj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (annotation != null && value != null) {
                if (type.equals(FilterTypeEnum.EQ)) {
                    eqMap.put(name, value);
                } else if (type.equals(FilterTypeEnum.LIKE)) {
                    likeMap.put(name, value);
                } else if (type.equals(FilterTypeEnum.LEFT)) {
                    leftMap.put(name, value);
                } else if (type.equals(FilterTypeEnum.RIGHT)) {
                    rightMap.put(name, value);
                } else if (type.equals(FilterTypeEnum.LIST)) {
                    if (!listField.equals("")) {
                        listMap.put(listField, value);
                    }
                }
            }

        }
        filterMap.put("eq", eqMap);
        filterMap.put("like", likeMap);
        filterMap.put("left", leftMap);
        filterMap.put("right", rightMap);
        filterMap.put("list", listMap);
        return filterMap;
    }

    public Criteria getCriterizByAnnotation(Object oj, TimeFilterModel tfModerl, String timeFiled) {
        if (timeFiled == null) {
            timeFiled = "updateTime";
        }
        Map<String, Object> filterField = this.getFilterField(oj);
        Criteria criteria = new Criteria();
        Map<String, Object> eqMap = (Map<String, Object>) filterField.get("eq");
        if (!isEmpty(eqMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : eqMap.entrySet()) {
                if (!isEmpty(stringObjectEntry.getValue())) {
                    criteria = criteria.and(stringObjectEntry.getKey()).is(stringObjectEntry.getValue());
                }
            }
        }
        Map<String, Object> likeMap = (Map<String, Object>) filterField.get("like");
        if (!isEmpty(likeMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : likeMap.entrySet()) {
                if (!isEmpty(stringObjectEntry.getValue())) {
                    criteria = criteria.and(stringObjectEntry.getKey()).regex(stringObjectEntry.getValue().toString(), "i");
                }
            }
        }

        Map<String, Object> leftMap = (Map<String, Object>) filterField.get("left");
        if (!isEmpty(leftMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : leftMap.entrySet()) {
                if (!isEmpty(stringObjectEntry.getValue())) {
                    criteria = criteria.and(stringObjectEntry.getKey()).regex("^" + stringObjectEntry.getValue().toString(), "i");
                }
            }
        }

        Map<String, Object> rightqMap = (Map<String, Object>) filterField.get("right");
        if (!isEmpty(rightqMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : rightqMap.entrySet()) {
                if (!isEmpty(stringObjectEntry.getValue())) {
                    criteria = criteria.and(stringObjectEntry.getKey()).regex(stringObjectEntry.getValue().toString() + "$", "i");
                }
            }
        }
        Map<String, Object> listMap = (Map<String, Object>) filterField.get("list");
        if (!isEmpty(listMap)) {
            for (Map.Entry<String, Object> stringObjectEntry : listMap.entrySet()) {
                if (!isEmpty(stringObjectEntry.getValue())) {
                    //被注解的字段的值
                    Object value = stringObjectEntry.getValue();
                    if (value instanceof List) {
                        List listValue = (List<Object>) value;
                        if (listValue.size() > 0) {
                            criteria = criteria.and(stringObjectEntry.getKey()).in(listValue);
                        }
                    }
                }
            }
        }

        //如果有时间筛选才参数，根据时间进行筛选
        if (!ObjectUtils.isEmpty(tfModerl)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sTime = tfModerl.getSTime();
            String eTime = tfModerl.getETime();
            try {
                Date s = sdf.parse(sTime);
                Date e = sdf.parse(eTime);
                if (!ObjectUtils.isEmpty(sTime)) {
                    criteria = criteria.and(timeFiled).gte(s);
                    if (!ObjectUtils.isEmpty(eTime)) {
                        criteria = criteria.lte(e);
                    }
                } else {
                    if (!ObjectUtils.isEmpty(eTime)) {
                        criteria = criteria.and(timeFiled).lte(e);
                    }
                }
            } catch (Exception e) {
                log.error("时间筛选错误:" + e.getMessage());
            }
        }

        return criteria;

    }

    public Criteria getCriterizByAnnotation(Object oj) {
        Criteria criterizByAnnotation = getCriterizByAnnotation(oj, null, null);
        return criterizByAnnotation;
    }

    public Criteria getCriterizByAnnotation(Object oj, TimeFilterModel tfModerl) {
        Criteria criterizByAnnotation = getCriterizByAnnotation(oj, tfModerl, null);
        return criterizByAnnotation;
    }

    public Query getMongoQueryByAnnotation(Object oj, TimeFilterModel tfModerl, String timeFiled) {
        Criteria criterizByAnnotation = getCriterizByAnnotation(oj, tfModerl, timeFiled);
        Query query = new Query(criterizByAnnotation);
        return query;
    }

    public Query getMongoQueryByAnnotation(Object oj, TimeFilterModel tfModerl) {
        Criteria criterizByAnnotation = getCriterizByAnnotation(oj, tfModerl, null);
        Query query = new Query(criterizByAnnotation);
        return query;
    }

    public Query getMongoQueryByAnnotation(Object oj) {
        Criteria criterizByAnnotation = getCriterizByAnnotation(oj, null, null);
        Query query = new Query(criterizByAnnotation);
        return query;
    }

    public Pageable getPageableByPageFilterModel(PageFilterModel pageFilterModel) {

        Integer pageNum = pageFilterModel.getPageNum();

        Integer pageSize = pageFilterModel.getPageSize();
        PageRequest pageRequest = PageRequest.of(pageNum, pageSize);
        return pageRequest;
    }

    public PageData<T> getPageByPageImpl(PageImpl pageimpl) {
        long totalElements = pageimpl.getTotalElements();
        List content = pageimpl.getContent();
        long number = Long.valueOf(pageimpl.getNumber()) + 1;
        long size = Long.valueOf(pageimpl.getSize());
        PageData pageData = new PageData<>(number, size, totalElements, content);
        return pageData;
    }

    public Criteria buildQueryByFilterMap(Map<String, String> filterMap, String filterPrefix, Criteria criteria) {

        if (filterMap != null && !filterMap.isEmpty()) {
            // 为所有过滤条件构建类型不敏感查询
            List<Criteria> filterCriteria = filterMap.entrySet().stream()
                    .map(entry -> buildTypeInsensitiveCriteria(filterPrefix + "." + entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            criteria = criteria.andOperator(filterCriteria.toArray(new Criteria[0]));
        }

        return criteria;
    }

    /**
     * 构建单个字段的类型不敏感条件 - 使用 Criteria 表达式
     */
    private Criteria buildTypeInsensitiveCriteria(String field, String value) {
        // 如果确实需要检查字段存在性，使用 and 操作符分开处理
        return new Criteria().andOperator(
                Criteria.where(field).exists(true),  // 单独检查字段存在性
                Criteria.where("$expr").is(
                        new org.bson.Document("$eq",
                                Arrays.asList(
                                        new org.bson.Document("$toString", "$" + field),
                                        value
                                )
                        )
                )
        );
    }
}
