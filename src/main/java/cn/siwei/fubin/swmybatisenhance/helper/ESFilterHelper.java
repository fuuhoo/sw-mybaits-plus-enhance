package cn.siwei.fubin.swmybatisenhance.helper;


import cn.siwei.fubin.swmybatisenhance.annotation.FilterFiled;
import cn.siwei.fubin.swmybatisenhance.constant.FilterTypeEnum;
import cn.siwei.fubin.swmybatisenhance.model.PageData;
import cn.siwei.fubin.swmybatisenhance.model.PageFilterModel;
import cn.siwei.fubin.swmybatisenhance.model.TimeFilterModel;
import cn.siwei.fubin.swmybatisenhance.util.ClassNameConvertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description: 针对 Elasticsearch 的查询参数拼接助手
 * @author fuhongbin (Optimized)
 */
@Component
@Slf4j
@ConditionalOnClass(name = "org.springframework.data.elasticsearch.core.ElasticsearchOperations")
public class ESFilterHelper<T> {

    /**
     * 解析对象中的注解并提取筛选字段
     */
    public Map<String, Object> getFilterField(Object oj) {
        Map<String, Object> filterMap = new HashMap<>();
        Map<String, Object> eqMap = new HashMap<>();
        Map<String, Object> likeMap = new HashMap<>();
        Map<String, Object> leftMap = new HashMap<>();
        Map<String, Object> rightMap = new HashMap<>();
        Map<String, Object> listMap = new HashMap<>();

        // 提前装载引用
        filterMap.put("eq", eqMap);
        filterMap.put("like", likeMap);
        filterMap.put("left", leftMap);
        filterMap.put("right", rightMap);
        filterMap.put("list", listMap);

        if (ObjectUtils.isEmpty(oj)) {
            return filterMap;
        }

        // 支持父类字段扫描，无视 private 限制
        ReflectionUtils.doWithFields(oj.getClass(), field -> {
            ReflectionUtils.makeAccessible(field);
            FilterFiled annotation = field.getAnnotation(FilterFiled.class);
            if (annotation == null) {
                return;
            }

            Object value = ReflectionUtils.getField(field, oj);
            if (ObjectUtils.isEmpty(value)) {
                return;
            }

            boolean b = annotation.ifCamel2UnderLine();
            String name = field.getName();
            String listField = annotation.listField();

            if (b) {
                name = ClassNameConvertUtil.toUnderScoreCase(name);
                listField = ClassNameConvertUtil.toUnderScoreCase(listField);
            }

            FilterTypeEnum type = annotation.type();

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
                default:
                    break;
            }
        });

        return filterMap;
    }

    /**
     * 将过滤属性转换为 Elasticsearch 的 Criteria
     */
    @SuppressWarnings("unchecked")
    public Criteria getCriteriaByAnnotation(Object oj, TimeFilterModel tfModerl, String timeFiled) {
        String finalTimeFiled = timeFiled == null ? "updateTime" : timeFiled;
        Map<String, Object> filterField = this.getFilterField(oj);

        // Spring Data ES 的 Criteria 构建机制与 MongoDB 略有不同，
        // 为了版本兼容，我们使用动态合并的机制
        Criteria rootCriteria = null;

        Map<String, Object> eqMap = (Map<String, Object>) filterField.get("eq");
        if (!ObjectUtils.isEmpty(eqMap)) {
            for (Map.Entry<String, Object> entry : eqMap.entrySet()) {
                rootCriteria = appendCriteria(rootCriteria, new Criteria(entry.getKey()).is(entry.getValue()));
            }
        }

        // ES 中的 contains, startsWith, endsWith 底层会自动转为 wildcard 或 match 查询
        Map<String, Object> likeMap = (Map<String, Object>) filterField.get("like");
        if (!ObjectUtils.isEmpty(likeMap)) {
            for (Map.Entry<String, Object> entry : likeMap.entrySet()) {
                rootCriteria = appendCriteria(rootCriteria, new Criteria(entry.getKey()).contains(entry.getValue().toString()));
            }
        }

        Map<String, Object> leftMap = (Map<String, Object>) filterField.get("left");
        if (!ObjectUtils.isEmpty(leftMap)) {
            for (Map.Entry<String, Object> entry : leftMap.entrySet()) {
                rootCriteria = appendCriteria(rootCriteria, new Criteria(entry.getKey()).startsWith(entry.getValue().toString()));
            }
        }

        Map<String, Object> rightMap = (Map<String, Object>) filterField.get("right");
        if (!ObjectUtils.isEmpty(rightMap)) {
            for (Map.Entry<String, Object> entry : rightMap.entrySet()) {
                rootCriteria = appendCriteria(rootCriteria, new Criteria(entry.getKey()).endsWith(entry.getValue().toString()));
            }
        }

        Map<String, Object> listMap = (Map<String, Object>) filterField.get("list");
        if (!ObjectUtils.isEmpty(listMap)) {
            for (Map.Entry<String, Object> entry : listMap.entrySet()) {
                Object v = entry.getValue();
                if (v instanceof Collection && !((Collection<?>) v).isEmpty()) {
                    rootCriteria = appendCriteria(rootCriteria, new Criteria(entry.getKey()).in((Collection<?>) v));
                }
            }
        }

        // ES 时间范围检索
        if (!ObjectUtils.isEmpty(tfModerl)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sTime = tfModerl.getSTime();
            String eTime = tfModerl.getETime();

            try {
                if (!ObjectUtils.isEmpty(sTime) && !ObjectUtils.isEmpty(eTime)) {
                    rootCriteria = appendCriteria(rootCriteria, new Criteria(finalTimeFiled).greaterThanEqual(sdf.parse(sTime)).lessThanEqual(sdf.parse(eTime)));
                } else if (!ObjectUtils.isEmpty(sTime)) {
                    rootCriteria = appendCriteria(rootCriteria, new Criteria(finalTimeFiled).greaterThanEqual(sdf.parse(sTime)));
                } else if (!ObjectUtils.isEmpty(eTime)) {
                    rootCriteria = appendCriteria(rootCriteria, new Criteria(finalTimeFiled).lessThanEqual(sdf.parse(eTime)));
                }
            } catch (ParseException e) {
                log.error("Elasticsearch 时间筛选解析错误: sTime={}, eTime={}", sTime, eTime, e);
            }
        }

        // 如果没有任何筛选条件，返回空的条件检索（避免空指针引发的构建异常）
        return rootCriteria == null ? new Criteria() : rootCriteria;
    }

    /**
     * 内部辅助方法：安全的动态拼接 ES Criteria
     */
    private Criteria appendCriteria(Criteria root, Criteria newCriteria) {
        return root == null ? newCriteria : root.and(newCriteria);
    }

    public Criteria getCriteriaByAnnotation(Object oj) {
        return getCriteriaByAnnotation(oj, null, null);
    }

    public Criteria getCriteriaByAnnotation(Object oj, TimeFilterModel tfModerl) {
        return getCriteriaByAnnotation(oj, tfModerl, null);
    }

    /**
     * 获取最终可直接用于 ElasticsearchOperations 查询的 CriteriaQuery
     */
    public CriteriaQuery getESQueryByAnnotation(Object oj, TimeFilterModel tfModerl, String timeFiled) {
        return new CriteriaQuery(getCriteriaByAnnotation(oj, tfModerl, timeFiled));
    }

    public CriteriaQuery getESQueryByAnnotation(Object oj, TimeFilterModel tfModerl) {
        return new CriteriaQuery(getCriteriaByAnnotation(oj, tfModerl, null));
    }

    public CriteriaQuery getESQueryByAnnotation(Object oj) {
        return new CriteriaQuery(getCriteriaByAnnotation(oj, null, null));
    }

    /**
     * 组装安全的分页对象 (0-indexed)
     */
    public Pageable getPageableByPageFilterModel(PageFilterModel pageFilterModel) {
        Integer pageNum = pageFilterModel.getPageNum();
        Integer pageSize = pageFilterModel.getPageSize();

        int pageIndex = (pageNum != null && pageNum > 0) ? pageNum - 1 : 0;
        int size = (pageSize != null && pageSize > 0) ? pageSize : 10;

        return PageRequest.of(pageIndex, size);
    }

    /**
     * 将 Elasticsearch 的 SearchPage 特殊分页对象转换为统一的 PageData
     */
    public PageData<T> getPageBySearchPage(SearchPage<T> searchPage) {
        long totalElements = searchPage.getTotalElements();
        // 从 SearchHits 中剥离出真实的实体对象
        List<T> content = searchPage.getSearchHits().getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        // 恢复 1-indexed 显示给前端
        long number = (long) searchPage.getNumber() + 1;
        long size = (long) searchPage.getSize();

        return new PageData<>(number, size, totalElements, content);
    }
}