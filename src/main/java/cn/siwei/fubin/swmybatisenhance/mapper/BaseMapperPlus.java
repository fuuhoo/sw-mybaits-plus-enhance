package cn.siwei.fubin.swmybatisenhance.mapper;


import cn.siwei.fubin.swmybatisenhance.util.BeanCopyUtils;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.baomidou.mybatisplus.extension.toolkit.Db.getOne;

public interface BaseMapperPlus<T> extends BaseMapper<T> {


    default List<T> selectList() {
        return this.selectList(new QueryWrapper<>());
    }

    /**
     * 批量插入
     */
    default boolean insertBatch(Collection<T> entityList) {
        return Db.saveBatch(entityList);
    }

    /**
     * 批量更新
     */
    default boolean updateBatchById(Collection<T> entityList) {
        return Db.updateBatchById(entityList);
    }

    /**
     * 批量插入或更新
     */
    default boolean insertOrUpdateBatch(Collection<T> entityList) {
        return Db.saveOrUpdateBatch(entityList);
    }

    /**
     * 批量插入(包含限制条数)
     */
    default boolean insertBatch(Collection<T> entityList, int batchSize) {
        return Db.saveBatch(entityList, batchSize);
    }

    /**
     * 批量更新(包含限制条数)
     */
    default boolean updateBatchById(Collection<T> entityList, int batchSize) {
        return Db.updateBatchById(entityList, batchSize);
    }

    /**
     * 批量插入或更新(包含限制条数)
     */
    default boolean insertOrUpdateBatch(Collection<T> entityList, int batchSize) {
        return Db.saveOrUpdateBatch(entityList, batchSize);
    }

    /**
     * 插入或更新(包含限制条数)
     */
    default boolean insertOrUpdate(T entity) {
        return Db.saveOrUpdate(entity);
    }


    /**
     * 根据Wrapper条件判断插入或更新
     * @param entity 实体对象
     * @param conditionWrapper 查询条件
     * @return 操作结果
     */
    default boolean insertOrUpdate(T entity, Wrapper<T> conditionWrapper) {
        if (entity == null) {
            return false;
        }

        if (conditionWrapper == null) {
            // 如果没有条件，使用默认的saveOrUpdate逻辑（基于主键）
            Object id = getEntityId(entity);
            if (id != null) {
                return updateById(entity) > 0;
            } else {
                return insert(entity) > 0;
            }
        }

        // 根据条件查询现有记录
        T existing = selectOne(conditionWrapper);

        if (existing != null) {
            // 存在则更新：复制主键并执行更新
            copyPrimaryKey(existing, entity);
            return updateById(entity) > 0;
        } else {
            // 不存在则插入
            return insert(entity) > 0;
        }
    }

    /**
     * 获取实体主键值
     */
    default Object getEntityId(T entity) {
        try {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(entity.getClass());
            if (tableInfo != null && tableInfo.havePK()) {
                return tableInfo.getPropertyValue(entity, tableInfo.getKeyProperty());
            }
        } catch (Exception e) {
            // 静默处理异常
        }
        return null;
    }

    /**
     * 复制主键值
     */
    default void copyPrimaryKey(T source, T target) {
        try {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(source.getClass());
            if (tableInfo != null && tableInfo.havePK()) {
                Object idValue = tableInfo.getPropertyValue(source, tableInfo.getKeyProperty());
                if (idValue != null) {
                    tableInfo.setPropertyValue(target, tableInfo.getKeyProperty(), idValue);
                }
            }
        } catch (Exception e) {
            // 静默处理异常
        }
    }

    /**
     * 根据 ID 查询
     */
    default <C> C selectVoById(Serializable id, Class<C> voClass) {
        T obj = this.selectById(id);
        if (ObjectUtils.isEmpty(obj)) {
            return null;
        }
        return BeanCopyUtils.copy(obj, voClass);
    }


    /**
     * 查询（根据ID 批量查询）
     */
    default <C> List<C> selectVoBatchIds(Collection<? extends Serializable> idList, Class<C> voClass) {
        List<T> list = this.selectBatchIds(idList);
        if (ObjectUtils.isEmpty(list)) {
            return Collections.EMPTY_LIST;
        }
        return BeanCopyUtils.copyList(list, voClass);
    }


    /**
     * 查询（根据 columnMap 条件）
     */
    default <C> List<C> selectVoByMap(Map<String, Object> map, Class<C> voClass) {
        List<T> list = this.selectByMap(map);
        if (ObjectUtils.isEmpty(list)) {
            return Collections.EMPTY_LIST;
        }
        return BeanCopyUtils.copyList(list, voClass);
    }



    /**
     * 根据 entity 条件，查询一条记录
     */
    default <C> C selectVoOne(Wrapper<T> wrapper, Class<C> voClass) {
        T obj = this.selectOne(wrapper);
        if (ObjectUtils.isEmpty(obj)) {
            return null;
        }
        return BeanCopyUtils.copy(obj, voClass);
    }


    /**
     * 根据 entity 条件，查询全部记录
     */
    default <C> List<C> selectVoList(Wrapper<T> wrapper, Class<C> voClass) {
        List<T> list = this.selectList(wrapper);
        if (ObjectUtils.isEmpty(list)) {
            return Collections.EMPTY_LIST;
        }
        return BeanCopyUtils.copyList(list, voClass);
    }


    /**
     * 分页查询VO
     */
    default <C, P extends IPage<C>> P selectVoPage(IPage<T> page, Wrapper<T> wrapper, Class<C> voClass) {
        IPage<T> pageData = this.selectPage(page, wrapper);
        IPage<C> voPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        if (ObjectUtils.isEmpty(pageData.getRecords())) {
            return (P) voPage;
        }
        voPage.setRecords(BeanCopyUtils.copyList(pageData.getRecords(), voClass));
        return (P) voPage;
    }

}
