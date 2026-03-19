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
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: 获取筛选字段的帮助类
 * @author fuhongbin
 * @date 2023/8/18 17:17
 */
@Component
@Slf4j
public class FilterWrapperHelper<T> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> getFilterField(Object oj) {
        Map<String, Object> filterMap = new HashMap<>();
        Map<String, Object> eqMap = new HashMap<>();
        Map<String, Object> likeMap = new HashMap<>();
        Map<String, Object> leftMap = new HashMap<>();
        Map<String, Object> rightMap = new HashMap<>();
        Map<String, Object> listMap = new HashMap<>();
        Map<String, Object> jsonListMap = new HashMap<>();
        Map<String, Object> jsonObjectMap = new HashMap<>();

        // 提前放置引用，避免最后重复 put
        filterMap.put("eq", eqMap);
        filterMap.put("like", likeMap);
        filterMap.put("left", leftMap);
        filterMap.put("right", rightMap);
        filterMap.put("list", listMap);
        filterMap.put("jsonList", jsonListMap);
        filterMap.put("jsonObj", jsonObjectMap);

        if (ObjectUtils.isEmpty(oj)) {
            return filterMap;
        }

        // 【优化】使用 ReflectionUtils 支持获取父类继承的字段，并替代冗余的 try-catch
        ReflectionUtils.doWithFields(oj.getClass(), declaredField -> {
            ReflectionUtils.makeAccessible(declaredField);
            FilterFiled annotation = declaredField.getAnnotation(FilterFiled.class);
            if (annotation == null) {
                return;
            }

            Object value = ReflectionUtils.getField(declaredField, oj);
            if (ObjectUtils.isEmpty(value)) {
                return;
            }

            boolean b = annotation.ifCamel2UnderLine();
            String name = declaredField.getName();
            String listField = annotation.listField();
            String aliasName = annotation.aliasName();

            if (b) {
                name = ClassNameConvertUtil.toUnderScoreCase(name);
                listField = ClassNameConvertUtil.toUnderScoreCase(listField);
                aliasName = ClassNameConvertUtil.toUnderScoreCase(aliasName);
            }

            FilterTypeEnum type = annotation.type();

            // 【优化】使用 switch 替代冗长的 if-else if 链
            switch (type) {
                case EQ:
                    eqMap.put(name, value);
                    break;
                case LIKE:
                    likeMap.put(name, value);
                    break;
                case LEFT:
                    leftMap.put(name, value);
                    break;
                case RIGHT:
                    rightMap.put(name, value);
                    break;
                case LIST:
                    if (!"".equals(listField)) {
                        listMap.put(listField, value);
                    }
                    break;
                case JSON_LIST:
                    // 【致命Bug修复】这里仅存储原始值，不再直接将 json 拼接到字符串中，防止 SQL 注入
                    jsonListMap.put(name, value);
                    break;
                case JSON_OBJ:
                    String jsonObjName = annotation.jsonObjName();
                    if (b) jsonObjName = ClassNameConvertUtil.toUnderScoreCase(jsonObjName);
                    String key1 = jsonObjName + "->>'$." + aliasName + "' > JSON_QUOTE({0})";
                    jsonObjectMap.put(key1, value);
                    break;
                case JSON_OBJ_LIST:
                    String jsonObjListName = annotation.jsonObjName();
                    if (b) jsonObjListName = ClassNameConvertUtil.toUnderScoreCase(jsonObjListName);
                    String key2 = "JSON_CONTAINS(" + jsonObjListName + ", JSON_OBJECT('" + aliasName + "', JSON_QUOTE({0})))";
                    jsonObjectMap.put(key2, value);
                    break;
                default:
                    break;
            }
        });

        return filterMap;
    }


    public Page<T> getPage(PageFilterModel pfm) {
        if (ObjectUtils.isEmpty(pfm)) {
            return new Page<>(1, 100);
        }
        return new Page<>(pfm.getPageNum(), pfm.getPageSize());
    }

    public LambdaQueryWrapper<T> getLambdaWrapperbyFiledAnnotion(Object oj) {
        QueryWrapper<T> tQueryWrapper = new QueryWrapper<>();
        return getQueryFilterWrapperbyAnotation(tQueryWrapper, oj).lambda();
    }

    public LambdaQueryWrapper<T> getLambdaWrapperbyFiledAnnotion(Object oj, TimeFilterModel tfModerl) {
        QueryWrapper<T> tQueryWrapper = new QueryWrapper<>();
        return getQueryFilterWrapperbyAnotation(tQueryWrapper, oj, tfModerl).lambda();
    }

    public LambdaQueryWrapper<T> getLambdaWrapperbyFiledAnnotion(Object oj, TimeFilterModel tfModerl, String timeFiled) {
        QueryWrapper<T> tQueryWrapper = new QueryWrapper<>();
        return getQueryFilterWrapperbyAnotation(tQueryWrapper, oj, tfModerl, timeFiled).lambda();
    }

    public void getBaseFilter(LambdaQueryWrapper<? extends BaseModel> tQueryWrapper, BaseFilterModel baseFilterModel) {
        String creatUser = baseFilterModel.getCreatUser();
        if (!ObjectUtils.isEmpty(creatUser)) {
            tQueryWrapper.eq(BaseModel::getCreateUser, creatUser);
        }

        String updateUser = baseFilterModel.getUpdateUser();
        if (!ObjectUtils.isEmpty(updateUser)) {
            tQueryWrapper.eq(BaseModel::getUpdateUser, updateUser);
        }

        Date creatTimeStart = baseFilterModel.getCreatTimeStart();
        if (!ObjectUtils.isEmpty(creatTimeStart)) {
            tQueryWrapper.ge(BaseModel::getCreateTime, creatTimeStart);
        }

        Date creatTimeEnd = baseFilterModel.getCreatTimeEnd();
        if (!ObjectUtils.isEmpty(creatTimeEnd)) {
            tQueryWrapper.le(BaseModel::getCreateTime, creatTimeEnd);
        }

        Date updateTimeStart = baseFilterModel.getUpdateTimeStart();
        if (!ObjectUtils.isEmpty(updateTimeStart)) {
            tQueryWrapper.ge(BaseModel::getUpdateTime, updateTimeStart);
        }

        Date updateTimeEnd = baseFilterModel.getUpdateTimeEnd();
        if (!ObjectUtils.isEmpty(updateTimeEnd)) {
            // 【逻辑修复】结束时间应该用 le (小于等于)，原代码错写成了 ge (大于等于)
            tQueryWrapper.le(BaseModel::getUpdateTime, updateTimeEnd);
        }
    }


    public QueryWrapper<T> getWrapperbyFiledAnnotion(Object oj, TimeFilterModel tfModerl, String timeFiled) {
        QueryWrapper<T> tQueryWrapper = new QueryWrapper<>();
        return getQueryFilterWrapperbyAnotation(tQueryWrapper, oj, tfModerl, timeFiled);
    }

    public QueryWrapper<T> getWrapperbyFiledAnnotion(Object oj, TimeFilterModel tfModerl) {
        QueryWrapper<T> tQueryWrapper = new QueryWrapper<>();
        return getQueryFilterWrapperbyAnotation(tQueryWrapper, oj, tfModerl, null);
    }

    public QueryWrapper<T> getWrapperbyFiledAnnotion(Object oj) {
        QueryWrapper<T> tQueryWrapper = new QueryWrapper<>();
        return getQueryFilterWrapperbyAnotation(tQueryWrapper, oj, null, null);
    }


    public QueryWrapper<T> getQueryFilterWrapperbyAnotation(QueryWrapper<T> qw, Object oj, TimeFilterModel tfModerl) {
        return getQueryFilterWrapperbyAnotation(qw, oj, tfModerl, null);
    }

    public QueryWrapper<T> getQueryFilterWrapperbyAnotation(QueryWrapper<T> qw, Object oj) {
        return getQueryFilterWrapperbyAnotation(qw, oj, null, null);
    }

    @SuppressWarnings("unchecked")
    public QueryWrapper<T> getQueryFilterWrapperbyAnotation(QueryWrapper<T> qw, Object oj, TimeFilterModel tfModerl, String timeFiled) {
        Map<String, Object> filterField = this.getFilterField(oj);
        String finalTimeFiled = timeFiled == null ? "updateTime" : timeFiled;

        // 【优化】使用 forEach 和 Lambda 表达式精简所有 Map 的遍历，大幅缩减代码量
        Map<String, Object> eqMap = (Map<String, Object>) filterField.get("eq");
        if (!ObjectUtils.isEmpty(eqMap)) {
            eqMap.forEach(qw::eq);
        }

        Map<String, Object> likeMap = (Map<String, Object>) filterField.get("like");
        if (!ObjectUtils.isEmpty(likeMap)) {
            likeMap.forEach(qw::like);
        }

        Map<String, Object> leftMap = (Map<String, Object>) filterField.get("left");
        if (!ObjectUtils.isEmpty(leftMap)) {
            leftMap.forEach(qw::likeLeft);
        }

        Map<String, Object> rightMap = (Map<String, Object>) filterField.get("right");
        if (!ObjectUtils.isEmpty(rightMap)) {
            rightMap.forEach(qw::likeRight);
        }

        Map<String, Object> listMap = (Map<String, Object>) filterField.get("list");
        if (!ObjectUtils.isEmpty(listMap)) {
            listMap.forEach((k, v) -> {
                // 【边界修复】防止 list 为空集合导致 Mybatis 生成 "IN ()" 报错
                if (v instanceof Collection && !((Collection<?>) v).isEmpty()) {
                    qw.in(k, (Collection<?>) v);
                }
            });
        }

        Map<String, Object> jsonlistMap = (Map<String, Object>) filterField.get("jsonList");
        if (!ObjectUtils.isEmpty(jsonlistMap)) {
            jsonlistMap.forEach((k, v) -> {
                try {
                    String jsonArray = objectMapper.writeValueAsString(v);
                    // 【致命Bug修复】结合上方 getFilterField 的修改，这里使用 MybatisPlus 原生 {0} 安全占位符来预编译 SQL 参数
                    qw.apply("JSON_OVERLAPS(" + k + ", CAST({0} AS JSON))", jsonArray);
                } catch (JsonProcessingException e) {
                    log.error("JSON转换失败, field: {}", k, e);
                }
            });
        }

        Map<String, Object> jsonObjMap = (Map<String, Object>) filterField.get("jsonObj");
        if (!ObjectUtils.isEmpty(jsonObjMap)) {
            // jsonObjMap 的 key 本身就是带 {0} 的安全 SQL 模板
            jsonObjMap.forEach(qw::apply);
        }

        // 如果有时间筛选才参数，根据时间进行筛选
        if (!ObjectUtils.isEmpty(tfModerl)) {
            String sTime = tfModerl.getSTime();
            String eTime = tfModerl.getETime();
            if (!ObjectUtils.isEmpty(sTime)) {
                qw.ge(finalTimeFiled, sTime);
            }
            if (!ObjectUtils.isEmpty(eTime)) {
                qw.le(finalTimeFiled, eTime);
            }
        }

        return qw;
    }
}