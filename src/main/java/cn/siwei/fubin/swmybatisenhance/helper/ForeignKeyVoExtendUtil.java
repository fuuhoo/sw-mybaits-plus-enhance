package cn.siwei.fubin.swmybatisenhance.helper;

import cn.siwei.fubin.swmybatisenhance.annotation.ConditionalDictVoExtend;
import cn.siwei.fubin.swmybatisenhance.annotation.SysDictVoExtend;
import cn.siwei.fubin.swmybatisenhance.annotation.VoExtend;
import cn.siwei.fubin.swmybatisenhance.annotation.VoExtendList;
import cn.siwei.fubin.swmybatisenhance.constant.VoExtendType;
import cn.siwei.fubin.swmybatisenhance.mapper.EmptyMapper;
import cn.siwei.fubin.swmybatisenhance.model.PageData;
import cn.siwei.fubin.swmybatisenhance.util.BeanCopyUtils;
import cn.siwei.fubin.swmybatisenhance.util.ReflectUtil;
import cn.siwei.fubin.swmybatisenhance.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import cn.siwei.fubin.swmybatisenhance.util.SpringContextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ForeignKeyVoExtendUtil {

    public static <T, V> V copy(T source, Class<V> desc) {
        if (ObjectUtils.isEmpty(source)) {
            return null;
        }
        if (ObjectUtils.isEmpty(desc)) {
            return null;
        }
        final V target = ReflectUtil.newInstanceIfPossible(desc);
        return copy(source, target);
    }


    public static <T, V> V copy(T source, V desc) {

        if (ObjectUtils.isEmpty(source)) {
            return null;
        }
        if (ObjectUtils.isEmpty(desc)) {
            return null;
        }

        BeanCopyUtils.BeanCopier beanCopier = BeanCopyUtils.BeanCopierCache.INSTANCE.get(source.getClass(), desc.getClass(), null);
        beanCopier.copy(source, desc, null);
        return desc;
    }

    public static <T, V> V getExtendName(T source, Class<V> desc) {
        ArrayList<T> ts = new ArrayList<>();
        ts.add(source);
        List<V> extendNameList = getExtendNameList(ts, desc);
        if (ObjectUtils.isEmpty(extendNameList)) {
            return ReflectUtil.newInstanceIfPossible(desc);
        }
        return extendNameList.get(0);
    }


    public static <T, V> PageData<V> getExtendNamePageData(Page<T> sourceList, Class<V> desc) {

        List<T> dataList = sourceList.getRecords();
        List<V> extendNameList = getExtendNameList(dataList, desc);

        PageData<V> vpageData = new PageData<>();

        vpageData.setSize(sourceList.getSize());
        vpageData.setCurrent(sourceList.getCurrent());
        vpageData.setTotalCount(sourceList.getTotal());

        if (ObjectUtils.isEmpty(extendNameList)) {
            extendNameList = new ArrayList<>();
        }
        vpageData.setDataList(extendNameList);

        return vpageData;


    }


    /**
     * @description: todo：批量设置字典的外键数据
     **/
    public static <T, V> List<V> getExtendNameList(List<T> sourceList, Class<V> desc) {


        if (ObjectUtils.isEmpty(sourceList)) {
            return Collections.EMPTY_LIST;
        }

        //要返回的新的List对象
        List<V> voList = BeanCopyUtils.copyList(sourceList, desc);
        List<Field> voFieldList = ReflectionKit.getFieldList(desc);
        //循环处理每个字段
        for (Field voFiled : voFieldList) {
            voFiled.setAccessible(true);
            //普通外键的
            VoExtend VoExtendAno = voFiled.getAnnotation(VoExtend.class);
            //根据注解获取需要形成的key和value
            //都为空则返回
            if (!ObjectUtils.isEmpty(VoExtendAno)) {
                dealVoExtend(sourceList, VoExtendAno, voFiled, voList);
            }

            //数据字典的统一处理
            SysDictVoExtend sysDictVoExtend = voFiled.getAnnotation(SysDictVoExtend.class);
            if (!ObjectUtils.isEmpty(sysDictVoExtend)) {
                SysDictVoExtend(sourceList, sysDictVoExtend, voFiled, voList);
            }

            ConditionalDictVoExtend annotation = voFiled.getAnnotation(ConditionalDictVoExtend.class);
            if (!ObjectUtils.isEmpty(annotation)) {
                ConditionalDictVoExtend(sourceList, annotation, voFiled, voList);
            }

            //自动填充list
            VoExtendList VoExtendListAno = voFiled.getAnnotation(VoExtendList.class);
            //不为空
            if (!ObjectUtils.isEmpty(VoExtendListAno)) {
                SysDictVoExtendList(sourceList, VoExtendListAno, voFiled, voList);
            }

        }
        return voList;
    }


    private static <T, V> void dealVoExtend(List<T> sourceList, VoExtend VoExtendAno, Field voFiled, List<V> voList) {
        //本身的id名称
        String id = VoExtendAno.foreignKeyForIdField();
        //map的名称
        Class mpClass = VoExtendAno.mapperName();
        //注解中的拓展对象的id名称
        String eId = VoExtendAno.extIdField();
        //数据库id名称
        String eDbID = VoExtendAno.extDbIdField();
        //数据库名称
        String eDbName = VoExtendAno.extDbNameField();
        boolean ifCamel2UnderLine = VoExtendAno.ifCamel2UnderLine();
        String eName = VoExtendAno.foreignModelFieldForName();
        VoExtendType type = VoExtendAno.type();

        //如果没填就认为和字段名一样
        if (ifCamel2UnderLine) {
            eDbID = eDbID.equals("") ? StringUtils.camel4underline(eId) : eDbID;
            eDbName = eDbName.equals("") ? StringUtils.camel4underline(eName) : eDbName;

        } else {
            eDbID = eDbID.equals("") ? eId : eDbID;
            eDbName = eDbName.equals("") ? eName : eDbName;
        }


        List<Object> collect = sourceList.stream()
                .map(e -> getFieldValue(e, id))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (!ObjectUtils.isEmpty(collect)) {
            //第一个值是什么类型
            Object idValue = collect.get(0);
            String typeName = voFiled.getType().getTypeName();
            //拿到id的值，id类型为数值或者string的时候起作用
            if (idValue instanceof Number || idValue instanceof String) {
                try {
                    //通过id拿到拓展中的对象列表
                    //拿到select方法
                    //拿到原对象中id对应的对象
                    Method selectMethod = mpClass.getMethod("selectList", Wrapper.class);
                    QueryWrapper<Object> wpper = new QueryWrapper<>();
                    wpper.in(eDbID, collect);
                    Object bean = SpringContextUtils.getBean(mpClass);
                    //通过select，拿到id的所有的对象
                    List<Object> invokeList = (List<Object>) selectMethod.invoke(bean, wpper);
                    String getMethodId = StringUtils.getGetMethodName(eId);
                    //获取id的反射方法，把对应的名称和对象放到map中
                    HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
                    HashMap<Object, Object> objectObjectHashMap2 = new HashMap<>();
                    //循环对象，
                    //如果是名称则放到map
                    //否则放到map2
                    for (Object o : invokeList) {

                        Class<?> aClass1 = o.getClass();
                        Method method1 = aClass1.getMethod(getMethodId);
                        String key = method1.invoke(o).toString();

                        if (type.equals(VoExtendType.Str)) {
                            //得到name值
                            Object value = getFieldValue(o, eName);
                            objectObjectHashMap.put(key, value);
                        }
                        objectObjectHashMap2.put(key, o);
                    }
                    //循环要返回的对象
                    //如果是名称，则返回名称
                    //如果是对象。则返回对象
                    for (V v : voList) {
                        String getMethodOID = StringUtils.getGetMethodName(id);
                        Class<?> aClass = v.getClass();
                        Method method = aClass.getMethod(getMethodOID);
                        //拿到每个对象的id
                        Object value = method.invoke(v);
                        //如果id不为空
                        if (!ObjectUtils.isEmpty(value)) {
                            if (type.equals(VoExtendType.OBJECT)) {
                                Object orDefaultValue = objectObjectHashMap2.getOrDefault(value.toString(), null);
                                voFiled.set(v, orDefaultValue);
                            } else if (type.equals(VoExtendType.Str)) {
                                Object orDefaultValue = objectObjectHashMap.getOrDefault(value.toString(), null);
                                voFiled.set(v, orDefaultValue);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("ForeignKeyVoExtendUtil对象转换出错:", e);
                }
            }

        }
    }

    private static <T, V> void SysDictVoExtend(List<T> sourceList, SysDictVoExtend sysDictVoExtend, Field voFiled, List<V> voList) {
        //mapper类
        Class mpClass = sysDictVoExtend.mapperName();
        //字典类型
        String dictType = sysDictVoExtend.sysDictType();
        //外键的字段
        String ori_id = sysDictVoExtend.foreignKeyIdField();
        //字典的id字段
        String sysIdFiedName = sysDictVoExtend.sysDictKeyFiedName();
        //字典的名称字段
        String labelFiedName = sysDictVoExtend.sysDictValueFiedName();
        //字典的类型
        String dictTypeFiedName = sysDictVoExtend.sysDictTypeFiedName();

        //数据库是否是驼峰转下划线
        boolean ifCamel2UnderLine = sysDictVoExtend.ifCamel2UnderLine();

        if (ifCamel2UnderLine) {
            dictTypeFiedName = StringUtils.camel4underline(dictTypeFiedName);
        }
        // 替换前几十行繁琐的 try-catch，一行搞定：
        List<Object> collect = sourceList.stream()
                .map(e -> getFieldValue(e, ori_id))
                .filter(Objects::nonNull) // 必须过滤空值
                .distinct() // 顺手去重，提升SQL性能
                .collect(Collectors.toList());
        // 增加集合判空，如果都没有字典值，直接结束该字段的映射
        if (ObjectUtils.isEmpty(collect)) {
            return;
        }

        //从字典查询出来所有的id对应的对象
        try {
            Method selectMethod = mpClass.getMethod("selectList", Wrapper.class);
            QueryWrapper<Object> wpper = new QueryWrapper<>();
            wpper.in(sysIdFiedName, collect);
            wpper.eq(dictTypeFiedName, dictType);
            Object bean = SpringContextUtils.getBean(mpClass);
            //获取sysdict列表
            List<Object> invokeList = (List<Object>) selectMethod.invoke(bean, wpper);
            //形成id-名称的字典
            HashMap<Object, Object> dictMap = new HashMap<>();
            for (Object o : invokeList) {
                Object key = getFieldValue(o, sysIdFiedName);
                Object value = getFieldValue(o, labelFiedName);
                if (key != null) {
                    dictMap.put(key.toString(), value);
                }
            }
            //循环赋值
            for (V vo : voList) {
                Object value = getFieldValue(vo, ori_id); // 一行搞定
                if (value == null) continue;
                Object orDefault = dictMap.getOrDefault(String.valueOf(value), null);
                voFiled.set(vo, orDefault);

            }
        } catch (Exception e) {
            log.error("字典赋值出错:" + e.getMessage());
        }
    }

    private static <T, V> void ConditionalDictVoExtend(List<T> sourceList, ConditionalDictVoExtend sysDictVoExtend, Field voFiled, List<V> voList) {
        //mapper类
        Class mpClass = sysDictVoExtend.mapperName();
        //外键的字段
        String foreignKeyFiledName = sysDictVoExtend.foreignKeyIdField();
        //字典的id字段
        String sysIdFiedName = sysDictVoExtend.sysDictKeyFiedName();
        //字典的名称字段
        String labelFiedName = sysDictVoExtend.sysDictValueFiedName();
        //字典的类型
        String dictTypeFiedName = sysDictVoExtend.sysDictTypeFiedName();

        String dictTypeFiedNameMethod = dictTypeFiedName;

        //数据库是否是驼峰转下划线
        boolean ifCamel2UnderLine = sysDictVoExtend.ifCamel2UnderLine();
        String[] strings = sysDictVoExtend.valueMappings();
        String dependentField = sysDictVoExtend.dependentField();
        if (ifCamel2UnderLine) {
            dictTypeFiedName = StringUtils.camel4underline(dictTypeFiedName);
        }


        Set<String> dependIdValueSet = sourceList.stream()
                .map(e -> getFieldValue(e, dependentField))
                .filter(Objects::nonNull) // 先过滤 null
                .map(Object::toString)    // 再转换为 String
                .collect(Collectors.toSet());

        ArrayList<String> dictTypeSet = new ArrayList<>();
        //根据dependentField查询所有对应类型
        if (!ObjectUtils.isEmpty(dependIdValueSet)) {
            for (String value : dependIdValueSet) {
                String dictTypeStr = resolveDictType(sysDictVoExtend, value);
                if (!ObjectUtils.isEmpty(dictTypeStr)) {
                    dictTypeSet.add(dictTypeStr);
                }
            }
        }

        try {
            //从数据库查询对应的类型的所有数据，由于不好获取每个类型对应的值，把所有类型的数据都取出来
            Method selectMethod = mpClass.getMethod("selectList", Wrapper.class);
            QueryWrapper<Object> wpper = new QueryWrapper<>();
            if (ObjectUtils.isEmpty(dictTypeSet)) {
                return;
            }
            wpper.in(dictTypeFiedName, dictTypeSet);
            Object bean = SpringContextUtils.getBean(mpClass);
            //获取sysdict列表
            List<Object> dicItemList = (List<Object>) selectMethod.invoke(bean, wpper);
            //形成一个type:id-value的map

            HashMap<String, String> dictMap = new HashMap<>();
            for (Object o : dicItemList) {
                Object type = getFieldValue(o, dictTypeFiedNameMethod);
                Object value = getFieldValue(o, labelFiedName);
                if (value == null) continue;

                Object key = getFieldValue(o, sysIdFiedName);
                if (type != null && key != null) {
                    dictMap.put(type.toString() + ":" + key.toString(), value.toString());
                }
            }

            //循环赋值
            for (V vo : voList) {
                Class<?> aClass = vo.getClass();

                Object dependTypeValue = getFieldValue(vo, dependentField);
                Object foreignKeyValue = getFieldValue(vo, foreignKeyFiledName);

                String dependentTypeFieldId = StringUtils.getGetMethodName(dependentField);
                Method dependTypeFieldGetMethod = aClass.getMethod(dependentTypeFieldId);
                String dependTypeName = resolveDictType(sysDictVoExtend, String.valueOf(dependTypeValue));

                String foreignKeyGetMethodStr = StringUtils.getGetMethodName(foreignKeyFiledName);
                Method foreignKeyGetMethod = aClass.getMethod(foreignKeyGetMethodStr);

                //为空不处理，
                if (ObjectUtils.isEmpty(dependTypeName)) {
                    continue;
                }
                Object orDefault = dictMap.getOrDefault(dependTypeName + ":" + foreignKeyValue, null);
                voFiled.set(vo, orDefault);

            }

        } catch (Exception e) {
            log.error("ForeignKeyVoExtendUtil出错:", e);
        }

    }

    private static <T, V> void SysDictVoExtendList(List<T> sourceList, VoExtendList VoExtendListAno, Field voFiled, List<V> voList) {
        Class<? extends BaseMapper> relationMapperClass = VoExtendListAno.relationMapper();
        String typeName = voFiled.getType().getTypeName();
        //关联表中的selfid
        String relationSelfIdAno = VoExtendListAno.relationSelfId();
        //关联表中的extid
        String relationExtIdAno = VoExtendListAno.relationExtId();

        String extNameFieldStr = VoExtendListAno.extNameField();

        boolean manytoMany = (relationMapperClass != EmptyMapper.class);

        if (List.class.isAssignableFrom(voFiled.getType()) && (!ObjectUtils.isEmpty(VoExtendListAno))) {
            //本身id名称
            String selfId = VoExtendListAno.selfModelIdField();
            //map的名称
            Class extMapperClass = VoExtendListAno.extMapper();
            //注解中的拓展对象的id名称
            String selfIdInExtModel = VoExtendListAno.selfIdInExtModel();
            //要显示的列表的名称，如果是对象则用不到
            String extName = VoExtendListAno.extNameField();
            String extModelIdFiledName = VoExtendListAno.extModelIdField();
            //要显示整个对象还是名称字符串
            VoExtendType type = VoExtendListAno.type();
            //extModel的id
            String extModelIdStr = VoExtendListAno.extModelIdField();

            boolean ifCamel2UnderLine = VoExtendListAno.ifCamel2UnderLine();

            Set<Object> selfIdSet = sourceList.stream()
                    .map(e -> getFieldValue(e, selfId))
                    .filter(Objects::nonNull) // 过滤null
                    .collect(Collectors.toSet()); // 这里保持 Object 类型即可，不要 toString
            //1对多
            if (manytoMany == false) {
                //id list不为空
                if (!ObjectUtils.isEmpty(selfIdSet)) {
                    try {
                        //一对多的关系
                        //构造查询条件
                        Method selectMethod = extMapperClass.getMethod("selectList", Wrapper.class);
                        QueryWrapper<Object> wrapper = new QueryWrapper<>();

                        String dBSelfIdInExtModel = selfIdInExtModel;
                        //转下划线
                        if (ifCamel2UnderLine) {
                            dBSelfIdInExtModel = StringUtils.camel4underline(selfIdInExtModel);
                        }

                        wrapper.in(dBSelfIdInExtModel, selfIdSet);
                        //获取class对象
                        Object classBean = SpringContextUtils.getBean(extMapperClass);
                        //通过select，拿到extModel
                        List<Object> extModelList = (List<Object>) selectMethod.invoke(classBean, wrapper);

                        //获取id的反射方法，把对应的名称和对象放到map中
                        HashMap<Object, List<Object>> strMap = new HashMap<>();
                        HashMap<Object, List<Object>> objMap = new HashMap<>();
                        //循环对象，
                        //如果是名称则放到map
                        //否则放到map2
                        //根据再extModel中的selfid字段，获取get方法

                        for (Object extItem : extModelList) {
                            // 直接使用封装好的 getFieldValue，抛弃繁琐的反射 getMethod
                            Object selfIdValueInExtModel = getFieldValue(extItem, selfIdInExtModel);
                            if (selfIdValueInExtModel == null) {
                                continue;
                            }
                            String selfIdValueInExtModelKey = selfIdValueInExtModel.toString();

                            if (type.equals(VoExtendType.Str)) {
                                // 直接取名称值
                                Object nameValue = getFieldValue(extItem, extName);
                                // 【性能优化】使用 computeIfAbsent 避免两次 Hash 计算，一行搞定 List 初始化和添加
                                strMap.computeIfAbsent(selfIdValueInExtModelKey, k -> new ArrayList<>()).add(nameValue);
                            } else if (type.equals(VoExtendType.OBJECT)) {
                                // 一行搞定 List 初始化和添加
                                objMap.computeIfAbsent(selfIdValueInExtModelKey, k -> new ArrayList<>()).add(extItem);
                            }
                        }

                        //循环赋值
                        for (V vo : voList) {
                            //拿到每个对象的id
                            Object id = getFieldValue(vo, selfId);
                            //如果id不为空
                            if (!ObjectUtils.isEmpty(id)) {
                                if (type.equals(VoExtendType.OBJECT)) {
                                    Object orDefaultValue = objMap.getOrDefault(id.toString(), Collections.emptyList());
                                    voFiled.set(vo, orDefaultValue);
                                } else if (type.equals(VoExtendType.Str)) {
                                    Object orDefaultValue = strMap.getOrDefault(id.toString(), Collections.emptyList());
                                    voFiled.set(vo, orDefaultValue);
                                }
                            }
                        }

                    } catch (Exception e) {
                        log.error("ForeignKeyVoExtendUtil出错:", e);
                        e.printStackTrace();
                    }

                }


            } else {
                //多对多
                if (!ObjectUtils.isEmpty(selfIdSet)) {
                    try {
                        Method relationSelectMethod = relationMapperClass.getMethod("selectList", Wrapper.class);
                        QueryWrapper<Object> wrapper = new QueryWrapper<>();
//                            //获取get方法
//                            String getSelfIdInRelationGetMethodStr = StringUtils.getGetMethodName(relationSelfIdAno);
                        //转下划线
                        String relationSelfIdUnderLine = relationSelfIdAno;
                        if (ifCamel2UnderLine) {
                            relationSelfIdUnderLine = StringUtils.camel4underline(relationSelfIdAno);
                        }

                        wrapper.in(relationSelfIdUnderLine, selfIdSet);
                        //获取class对象
                        Object relationClassBean = SpringContextUtils.getBean(relationMapperClass);
                        //通过select，拿到extModel
                        List<Object> ralatioModelList = (List<Object>) relationSelectMethod.invoke(relationClassBean, wrapper);

                        // 必须从中间表集合 ralatioModelList 获取关联的目标外键ID
                        Set<Object> extModelIdSet = ralatioModelList.stream()
                                .map(e -> getFieldValue(e, relationExtIdAno))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());


                        // 关联关系分组 (替换原来 40 多行的 try-catch)
                        Map<Object, List<Object>> selfId_extId_Map = ralatioModelList.stream()
                                .filter(e -> getFieldValue(e, relationSelfIdAno) != null) // 必须加这个过滤
                                .collect(Collectors.groupingBy(
                                        e -> getFieldValue(e, relationSelfIdAno),
                                        Collectors.mapping(
                                                e -> getFieldValue(e, relationExtIdAno),
                                                Collectors.toList()
                                        )
                                ));

                        //查询所有的model
                        Method extMapperselectMethod = extMapperClass.getMethod("selectList", Wrapper.class);
                        QueryWrapper<Object> extModelWrapper = new QueryWrapper<>();

                        //转下划线
                        if (ifCamel2UnderLine) {
                            extModelIdStr = StringUtils.camel4underline(extModelIdStr);
                        }

                        if (extModelIdSet.isEmpty()) {
                            return; // 如果关联关系中没有有效的目标ID，直接终止后续查询
                        }
                        extModelWrapper.in(extModelIdStr, extModelIdSet);
                        //获取class对象
                        Object extMapperObj = SpringContextUtils.getBean(extMapperClass);
                        //通过select，拿到extModel
                        List<Object> extModelList = (List<Object>) extMapperselectMethod.invoke(extMapperObj, extModelWrapper);

                        //获取id的反射方法，把对应的名称和对象放到map中
                        HashMap<Object, List<Object>> strMap = new HashMap<>();
                        HashMap<Object, List<Object>> objMap = new HashMap<>();


                        // 1. 【性能优化】O(N) 将目标实体列表转化为 Map (extModelId -> extModelObj)，实现 O(1) 极速查找
                        Map<Object, Object> extModelMap = extModelList.stream()
                                .filter(m -> getFieldValue(m, extModelIdFiledName) != null)
                                .collect(Collectors.toMap(
                                        m -> getFieldValue(m, extModelIdFiledName),
                                        m -> m,
                                        (v1, v2) -> v1 // 遇到重复ID时保留第一个
                                ));

                        // 2. 【性能优化】直接遍历分组好的关联关系，去 Map 中精确提取目标对象，消除多重嵌套循环
                        for (Map.Entry<Object, List<Object>> entry : selfId_extId_Map.entrySet()) {
                            String selfKey = entry.getKey().toString();
                            List<Object> relatedExtIdList = entry.getValue();

                            for (Object extId : relatedExtIdList) {
                                Object modelObj = extModelMap.get(extId); // O(1) 极速提取对象
                                if (modelObj != null) {
                                    if (type.equals(VoExtendType.Str)) {
                                        Object nameValue = getFieldValue(modelObj, extName);
                                        // 极简写法：使用 computeIfAbsent 代替 getOrDefault + if-else
                                        strMap.computeIfAbsent(selfKey, k -> new ArrayList<>()).add(nameValue);
                                    } else if (type.equals(VoExtendType.OBJECT)) {
                                        objMap.computeIfAbsent(selfKey, k -> new ArrayList<>()).add(modelObj);
                                    }
                                }
                            }
                        }

                        //循环赋值
                        for (V vo : voList) {
                            Object id = getFieldValue(vo, selfId);
                            //如果id不为空
                            if (!ObjectUtils.isEmpty(id)) {
                                if (type.equals(VoExtendType.OBJECT)) {
                                    Object orDefaultValue = objMap.getOrDefault(id.toString(), Collections.emptyList());
                                    voFiled.set(vo, orDefaultValue);
                                } else if (type.equals(VoExtendType.Str)) {
                                    Object orDefaultValue = strMap.getOrDefault(id.toString(), Collections.emptyList());
                                    voFiled.set(vo, orDefaultValue);
                                }

                            }
                        }

                    } catch (Exception e) {
                        log.error("ForeignKeyVoExtendUtil对象转换出错:", e);
                    }
                }


            }
        }
    }

    private static String resolveDictType(ConditionalDictVoExtend annotation, String dependentValue) {
        for (String mapping : annotation.valueMappings()) {
            String[] parts = mapping.split(":");
            if (parts.length == 2 && parts[0].equals(dependentValue)) {
                return parts[1];
            }
        }
        //没有则返回null
        return null;
    }

    // 新增私有工具方法，支持向上追溯父类字段
    private static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null || StringUtils.isEmpty(fieldName)) return null;
        Field field = org.springframework.util.ReflectionUtils.findField(obj.getClass(), fieldName);
        if (field != null) {
            org.springframework.util.ReflectionUtils.makeAccessible(field);
            return org.springframework.util.ReflectionUtils.getField(field, obj);
        }
        return null;
    }

}




