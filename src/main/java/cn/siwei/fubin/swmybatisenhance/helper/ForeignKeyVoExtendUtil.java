package cn.siwei.fubin.swmybatisenhance.helper;

import cn.siwei.fubin.swmybatisenhance.annotation.SysDictVoExtend;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.siwei.fubin.swmybatisenhance.annotation.VoExtend;
import cn.siwei.fubin.swmybatisenhance.annotation.VoExtendList;
import cn.siwei.fubin.swmybatisenhance.constant.VoExtendType;
import cn.siwei.fubin.swmybatisenhance.mapper.BaseMapperPlus;
import cn.siwei.fubin.swmybatisenhance.mapper.EmptyMapper;
import cn.siwei.fubin.swmybatisenhance.model.PageData;
import cn.siwei.fubin.swmybatisenhance.util.BeanCopyUtils;
import cn.siwei.fubin.swmybatisenhance.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import cn.siwei.fubin.swmybatisenhance.util.SpringContextUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ForeignKeyVoExtendUtil {

    public static <T, V> V copy(T source, Class<V> desc) {
        if (ObjectUtil.isNull(source)) {
            return null;
        }
        if (ObjectUtil.isNull(desc)) {
            return null;
        }
        final V target = ReflectUtil.newInstanceIfPossible(desc);
        return copy(source, target);
    }


    public static <T, V> V copy(T source, V desc) {
        if (ObjectUtil.isNull(source)) {
            return null;
        }
        if (ObjectUtil.isNull(desc)) {
            return null;
        }

        BeanCopier beanCopier = BeanCopyUtils.BeanCopierCache.INSTANCE.get(source.getClass(), desc.getClass(), null);
        beanCopier.copy(source, desc, null);
        return desc;

    }


    public static <T, V> V getExtendName(T source, Class<V> desc) {
        if (ObjectUtil.isNull(source)) {
            return null;
        }
        if (ObjectUtil.isNull(desc)) {
            return null;
        }
        //先复制原来有属性
        final V target = ReflectUtil.newInstanceIfPossible(desc);
        BeanCopier beanCopier = BeanCopyUtils.BeanCopierCache.INSTANCE.get(source.getClass(), desc, null);
        beanCopier.copy(source, target, null);

        List<Field> fieldList = ReflectionKit.getFieldList(desc);
        for (Field field : fieldList) {
            try {
                //设置字段可以被访问
                field.setAccessible(true);
                //字典的字段============================================================================start
                SysDictVoExtend sysDictVoExtend = field.getAnnotation(SysDictVoExtend.class);
                //如果存在字典的数据
                if (!ObjectUtils.isEmpty(sysDictVoExtend)) {

                    Class mpClass = sysDictVoExtend.mapperName();
                    String type = sysDictVoExtend.sysDictTypeName();
                    String ori_id = sysDictVoExtend.selfModelFileName();
                    String labelFiedName = sysDictVoExtend.sysDictValueFiedName();

                    QueryWrapper<Object> eq = new QueryWrapper<>().eq("dict_type", type)
                            .eq("code", ori_id);

                    Method selectList = mpClass.getMethod("selectOne", QueryWrapper.class, Boolean.class);

                    Object invoke = selectList.invoke(selectList, eq, false);

                    if (!ObjectUtils.isEmpty(invoke)) {
                        String getMethodName = StringUtils.getGetMethodName(labelFiedName);
                        Class<?> aClass1 = invoke.getClass();
                        Method method = aClass1.getMethod(getMethodName);
                        //得到name值
                        Object invoke1 = method.invoke(invoke);
                        field.set(target, invoke1);
                    }

                }
                //字典的字段============================================================================end


                //里面有VoExtend注解的字段
                VoExtend VoExtendAno = field.getAnnotation(VoExtend.class);
                //存在VoExtend注解
                if (!ObjectUtils.isEmpty(VoExtendAno)) {
                    //根据注解
                    // 获取id字段名称和name字段名称
                    String id = VoExtendAno.selfIdField();
                    VoExtendType type = VoExtendAno.type();
                    //mapperd 的类名
                    Class mpClass = VoExtendAno.mapperName();
                    //获取id值
                    Field declaredField = source.getClass().getDeclaredField(id);

                    Object idValue = declaredField.get(source);

                    //这部分是反向外键的
                    if (idValue instanceof List) {
                        Method selectList = mpClass.getDeclaredMethod("selectBatchIds", QueryWrapper.class);
                        QueryWrapper<Object> wpper = new QueryWrapper<>();
                        wpper.in(id, idValue);
                        Object bean = SpringContextUtils.getBean(mpClass);
                        Object invoke = (List<Class<?>>) selectList.invoke(bean, wpper);
                        field.set(target, invoke);
                        //这部分类似于外键
                    } else if (idValue instanceof Number || idValue instanceof String) {
                        Method selectByIdMethod = mpClass.getMethod("selectById", Serializable.class);
                        Object bean = SpringContextUtils.getBean(mpClass);
                        Object invoke = selectByIdMethod.invoke(bean, idValue);

                        if (ObjectUtils.isEmpty(invoke)) {
                            return target;
                        }
                        if (type.equals(VoExtendType.OBJECT)) {
                            field.set(target, invoke);
                        } else if (type.equals(VoExtendType.Str)) {
                            //获取字段名称
                            String extName = VoExtendAno.extNameField();
                            String getMethodName = StringUtils.getGetMethodName(extName);
                            Class<?> aClass1 = invoke.getClass();
                            Method method = aClass1.getMethod(getMethodName);
                            //得到name值
                            Object invoke1 = method.invoke(invoke);
                            //设置到属性
                            field.set(target, invoke1);
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();

            }

        }
        return target;
    }


    public static <T, V> PageData<V> getExtendNamePageData(Page<T> sourceList, Class<V> desc) {

        List<T> dataList = sourceList.getRecords();
        List<V> extendNameList = getExtendNameList(dataList, desc);

        PageData<V> vpageData = new PageData<>();

        vpageData.setSize(sourceList.getSize());
        vpageData.setCurrent(sourceList.getCurrent());
        vpageData.setTotalCount(sourceList.getTotal());

        vpageData.setDataList(extendNameList);

        return vpageData;


    }


    /**
     * @description: todo：批量设置字典的外键数据
     **/
    public static <T, V> List<V> getExtendNameList(List<T> sourceList, Class<V> desc) {

        if (ObjectUtil.isNull(sourceList)) {
            return null;
        }
        if (CollUtil.isEmpty(sourceList)) {
            return CollUtil.newArrayList();
        }
        //要返回的新的List对象
        List<V> voList = BeanCopyUtils.copyList(sourceList, desc);

        List<Field> voFieldList = ReflectionKit.getFieldList(desc);
        //获取extend的字段以及其id和name
        for (Field voFiled : voFieldList) {
            voFiled.setAccessible(true);
            //外键的
            VoExtend VoExtendAno = voFiled.getAnnotation(VoExtend.class);
            //根据注解获取需要形成的key和value
            //都为空则返回
            if (!ObjectUtils.isEmpty(VoExtendAno)) {
                //本身的id名称
                String id = VoExtendAno.selfIdField();
                //map的名称
                Class mpClass = VoExtendAno.mapperName();
                //注解中的拓展对象的id名称
                String eId = VoExtendAno.extIdField();
                String eDbID = VoExtendAno.extDbIdField();
                String eDbName = VoExtendAno.extDbNameField();
                String eName = VoExtendAno.extNameField();
                VoExtendType type = VoExtendAno.type();
                eDbID=eDbID.equals("")?eId:eDbID;
                eDbName=eDbName.equals("")?eName:eDbName;

                //原对象列表中获取原来所有id的，形成列表
                List<Object> collect = sourceList.stream().map(e -> {
                    //获取基类的id值
                    Field declaredField = null;
                    try {
                        declaredField = e.getClass().getDeclaredField(id);
                    } catch (NoSuchFieldException noSuchFieldException) {
                        noSuchFieldException.printStackTrace();
                    }
                    Object idValue = null;
                    try {
                        if (!ObjectUtils.isEmpty(declaredField)) {
                            declaredField.setAccessible(true);
                            idValue = declaredField.get(e);
                        }
                    } catch (IllegalAccessException illegalAccessException) {
                        illegalAccessException.printStackTrace();
                    }
                    return idValue;
                }).collect(Collectors.toList());


                if (!ObjectUtils.isEmpty(collect)) {

                    //第一个值是什么类型
                    Object idValue = collect.get(0);

                    String typeName = voFiled.getType().getTypeName();
                    //java.util.List

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
                                    String getMethodName = StringUtils.getGetMethodName(eName);
                                    Method method = aClass1.getMethod(getMethodName);
                                    //得到name值
                                    Object value = method.invoke(o);
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
                            e.printStackTrace();
                        }
                    }

                }
            }

            //字典的
            SysDictVoExtend sysDictVoExtend = voFiled.getAnnotation(SysDictVoExtend.class);
            if (!ObjectUtils.isEmpty(sysDictVoExtend)) {

                Class mpClass = sysDictVoExtend.mapperName();
                String dictType = sysDictVoExtend.sysDictTypeName();
                String ori_id = sysDictVoExtend.selfModelFileName();
                String sysIdFiedName = sysDictVoExtend.sysDictKeyFiedName();
                String labelFiedName = sysDictVoExtend.sysDictValueFiedName();
                String dictTypeFiedName = sysDictVoExtend.sysDictTypeFiedName();

                List<Object> collect = sourceList.stream().map(e -> {
                    //获取基类的id值
                    Field declaredField = null;
                    try {
                        declaredField = e.getClass().getDeclaredField(ori_id);
                    } catch (NoSuchFieldException noSuchFieldException) {
                        noSuchFieldException.printStackTrace();
                    }
                    Object idValue = null;
                    try {
                        if (!ObjectUtils.isEmpty(declaredField)) {
                            declaredField.setAccessible(true);
                            idValue = declaredField.get(e);
                        }
                    } catch (IllegalAccessException illegalAccessException) {
                        illegalAccessException.printStackTrace();
                    }
                    return idValue;
                }).collect(Collectors.toList());

                try {
                    Method selectMethod = mpClass.getMethod("selectList", Wrapper.class);
                    QueryWrapper<Object> wpper = new QueryWrapper<>();
                    wpper.in(sysIdFiedName, collect);
                    wpper.eq(dictTypeFiedName, dictType);
                    Object bean = SpringContextUtils.getBean(mpClass);

                    //获取sysdict列表
                    List<Object> invokeList = (List<Object>) selectMethod.invoke(bean, wpper);
//                    System.out.println(invokeList);
//                    //形成字典
//                    System.out.println(11);

                    HashMap<Object, Object> dictMap = new HashMap<>();

                    for (Object o : invokeList) {
                        Class<?> aClass = o.getClass();
                        String getMethodId = StringUtils.getGetMethodName(sysIdFiedName);
                        String getMethodName = StringUtils.getGetMethodName(labelFiedName);
                        Method methodName = aClass.getMethod(getMethodName);
                        Method MethodId = aClass.getMethod(getMethodId);
                        Object value = methodName.invoke(o);
                        Object key = MethodId.invoke(o);
                        dictMap.put(key, value);
                    }

                    for (V vo : voList) {
                        String getMethodOID = StringUtils.getGetMethodName(ori_id);
                        Class<?> aClass = vo.getClass();
                        Method method = aClass.getMethod(getMethodOID);
                        Object value = method.invoke(vo);

                        Object orDefault = dictMap.getOrDefault(value, null);
                        voFiled.set(vo, orDefault);
                    }


                } catch (Exception e) {
                    e.printStackTrace();

                }


            }

            //自动填充list
            VoExtendList VoExtendListAno = voFiled.getAnnotation(VoExtendList.class);
            //类型不是list，则跳过这次
            String typeName = voFiled.getType().getTypeName();
            if(ObjectUtils.isEmpty(VoExtendListAno)){
                continue;
            }
            Class<? extends BaseMapper> relationMapperClass = VoExtendListAno.relationMapper();

            //关联表中的selfid
            String relationSelfIdAno = VoExtendListAno.relationSelfId();
            //关联表中的extid
            String relationExtIdAno = VoExtendListAno.relationExtId();

            String extNameFieldStr = VoExtendListAno.extNameField();

            Boolean manytoMany=false;
            if(!relationMapperClass.toString().equals(EmptyMapper.class.toString())){
                manytoMany=true;
            }

            if (typeName.equals("java.util.List") && (!ObjectUtils.isEmpty(VoExtendListAno))) {
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

                //获取原对象列表中获取原来所有id的，形成列表
                Set<Object> selfIdSet = sourceList.stream().map(e -> {
                    //获取基类的id值
                    Field selfIdValue = null;
                    try {
                        selfIdValue = e.getClass().getDeclaredField(selfId);
                    } catch (NoSuchFieldException noSuchFieldException) {
                        noSuchFieldException.printStackTrace();
                    }
                    Object idValue = null;
                    try {
                        if (!ObjectUtils.isEmpty(selfIdValue)) {
                            selfIdValue.setAccessible(true);
                            idValue = selfIdValue.get(e);
                        }
                    } catch (IllegalAccessException illegalAccessException) {
                        illegalAccessException.printStackTrace();
                    }
                    return idValue;
                }).collect(Collectors.toSet());

                //1对多
                if (manytoMany == false) {
                    //id list不为空
                    if (!ObjectUtils.isEmpty(selfIdSet)) {
                        try {
                            //一对多的关系
                            //构造查询条件
                            Method selectMethod = extMapperClass.getMethod("selectList", Wrapper.class);
                            QueryWrapper<Object> wrapper = new QueryWrapper<>();

                            //转下划线
                           String dBSelfIdInExtModel = StringUtils.camel4underline(selfIdInExtModel);
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
                            String getSelfIdInExtModelGetMethodStr = StringUtils.getGetMethodName(selfIdInExtModel);

                            for (Object extItem : extModelList) {
                                //extmodel类
                                Class<?> extModelClass = extItem.getClass();
                                //extmodel获取selfid的方法，相当于根据selfId分组
                                Method getSelfIdValueInExtModelMethod = extModelClass.getMethod(getSelfIdInExtModelGetMethodStr);
                                String selfIdValueInExtModel = getSelfIdValueInExtModelMethod.invoke(extItem).toString();
                                if (type.equals(VoExtendType.Str)) {
                                    //获取得到name值
                                    String getMethodName = StringUtils.getGetMethodName(extName);
                                    Method method = extModelClass.getMethod(getMethodName);
                                    Object nameValue = method.invoke(extItem);
                                    List<Object> objects = strMap.getOrDefault(selfIdValueInExtModel, null);

                                    if (objects == null) {
                                        objects = new ArrayList<Object>();
                                        objects.add(nameValue);
                                        strMap.put(selfIdValueInExtModel, objects);
                                    } else {
                                        objects.add(nameValue);
                                    }
                                }
                                if (type.equals(VoExtendType.OBJECT)) {
                                //这部分用到了引用，直接add即可。
                                    List<Object> extModelListForMap = objMap.getOrDefault(selfIdValueInExtModel, null);
                                    if (extModelListForMap == null) {
                                        extModelListForMap = new ArrayList<Object>();
                                        extModelListForMap.add(extItem);
                                        objMap.put(selfIdValueInExtModel, extModelListForMap);
                                    } else {
                                        extModelListForMap.add(extItem);
                                    }
                                }
                            }


                            //循环赋值
                            for (V vo : voList) {
                                String getMethodOID = StringUtils.getGetMethodName(selfId);
                                Class<?> aClass = vo.getClass();
                                Method method = aClass.getMethod(getMethodOID);
                                //拿到每个对象的id
                                Object id = method.invoke(vo);
                                //如果id不为空
                                if (!ObjectUtils.isEmpty(id)) {
                                    if (type.equals(VoExtendType.OBJECT)) {
                                        Object orDefaultValue = objMap.getOrDefault(id.toString(), null);
                                        voFiled.set(vo, orDefaultValue);
                                    } else if (type.equals(VoExtendType.Str)) {
                                        Object orDefaultValue = strMap.getOrDefault(id.toString(), null);
                                        voFiled.set(vo, orDefaultValue);
                                    }
                                }
                            }

                        } catch (Exception e) {
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
                            String relationSelfIdUnderLine = StringUtils.camel4underline(relationSelfIdAno);
                            wrapper.in(relationSelfIdUnderLine, selfIdSet);
                            //获取class对象
                            Object relationClassBean = SpringContextUtils.getBean(relationMapperClass);
                            //通过select，拿到extModel
                            List<Object> ralatioModelList = (List<Object>) relationSelectMethod.invoke(relationClassBean, wrapper);

                            //拿到extModelIdSet
                            Set<Object> extModelIdSet = ralatioModelList.stream().map(e -> {
                                Field extModelIdField = null;
                                try {
                                    extModelIdField = e.getClass().getDeclaredField(relationExtIdAno);
                                } catch (NoSuchFieldException noSuchFieldException) {
                                    noSuchFieldException.printStackTrace();
                                }
                                Object extModelId = null;
                                try {
                                    if (!ObjectUtils.isEmpty(extModelIdField)) {
                                        extModelIdField.setAccessible(true);
                                        extModelId = extModelIdField.get(e);
                                    }
                                } catch (IllegalAccessException illegalAccessException) {
                                    illegalAccessException.printStackTrace();
                                }
                                return extModelId;
                            }).collect(Collectors.toSet());

                            //关联关系分组
                            Map<Object,List<Object>> selfId_extId_Map=ralatioModelList.stream().collect(Collectors.groupingBy(
                                    e->{
                                        Field selfModelIdField = null;
                                        try {
                                            selfModelIdField = e.getClass().getDeclaredField(relationSelfIdAno);
                                        } catch (NoSuchFieldException noSuchFieldException) {
                                            noSuchFieldException.printStackTrace();
                                        }
                                        Object selfModelId = null;
                                        try {
                                            if (!ObjectUtils.isEmpty(selfModelIdField)) {
                                                selfModelIdField.setAccessible(true);
                                                selfModelId = selfModelIdField.get(e);
                                            }
                                        } catch (IllegalAccessException illegalAccessException) {
                                            illegalAccessException.printStackTrace();
                                        }
                                        return selfModelId;

                                    },Collectors.mapping(
                                            e->{
                                                Field extModelIdField = null;
                                                try {
                                                    extModelIdField = e.getClass().getDeclaredField(relationExtIdAno);
                                                } catch (NoSuchFieldException noSuchFieldException) {
                                                    noSuchFieldException.printStackTrace();
                                                }
                                                Object extModelId = null;
                                                try {
                                                    if (!ObjectUtils.isEmpty(extModelIdField)) {
                                                        extModelIdField.setAccessible(true);
                                                        extModelId = extModelIdField.get(e);
                                                    }
                                                } catch (IllegalAccessException illegalAccessException) {
                                                    illegalAccessException.printStackTrace();
                                                }
                                                return extModelId;

                                            },
                                            Collectors.toList()
                                    )
                            ));

                            //查询所有的model
                            Method extMapperselectMethod = extMapperClass.getMethod("selectList", Wrapper.class);
                            QueryWrapper<Object> extModelWrapper = new QueryWrapper<>();
                            //转下划线
                            extModelIdStr = StringUtils.camel4underline(extModelIdStr);
                            extModelWrapper.in(extModelIdStr, extModelIdSet);
                            //获取class对象
                            Object extMapperObj = SpringContextUtils.getBean(extMapperClass);
                            //通过select，拿到extModel
                            List<Object> extModelList = (List<Object>) extMapperselectMethod.invoke(extMapperObj, extModelWrapper);

                            //获取id的反射方法，把对应的名称和对象放到map中
                            HashMap<Object, List<Object>> strMap = new HashMap<>();
                            HashMap<Object, List<Object>> objMap = new HashMap<>();

                            for (Object modelObj : extModelList) {
                                //获取modelId
                                Class<?> extModelClass = modelObj.getClass();
                                Field extModelIdField = null;
                                try {
                                    extModelIdField = extModelClass.getDeclaredField(extModelIdFiledName);
                                } catch (NoSuchFieldException noSuchFieldException) {
                                    noSuchFieldException.printStackTrace();
                                }
                                Object extModelId = null;
                                try {
                                    if (!ObjectUtils.isEmpty(extModelIdField)) {
                                        extModelIdField.setAccessible(true);
                                        extModelId = extModelIdField.get(modelObj);
                                    }
                                } catch (IllegalAccessException illegalAccessException) {
                                    illegalAccessException.printStackTrace();
                                }
                                if(!ObjectUtils.isEmpty(extModelId)) {
                                    for (Object key : selfId_extId_Map.keySet()) {
                                        //分组后的list
                                        List<Object> modelIdList = selfId_extId_Map.getOrDefault(key, null);
                                        if (!ObjectUtils.isEmpty(modelIdList)) {
                                            if (modelIdList.contains(extModelId)) {
                                                if (type.equals(VoExtendType.Str)) {
                                                    String getMethodName = StringUtils.getGetMethodName(extName);
                                                    Method method = extModelClass.getMethod(getMethodName);
                                                    Object nameValue = method.invoke(modelObj);
                                                    List<Object> extIdList = strMap.getOrDefault(key, null);
                                                    if (extIdList == null) {
                                                        extIdList = new ArrayList<Object>();
                                                        extIdList.add(nameValue);
                                                        strMap.put(key, extIdList);
                                                    } else {
                                                        extIdList.add(nameValue);
                                                    }

                                                }
                                                if (type.equals(VoExtendType.OBJECT)) {
                                                    List<Object> extModelListForMap = objMap.getOrDefault(key, null);
                                                    if (extModelListForMap == null) {
                                                        extModelListForMap = new ArrayList<Object>();
                                                        extModelListForMap.add(modelObj);
                                                        objMap.put(modelObj, extModelListForMap);
                                                    } else {
                                                        extModelListForMap.add(modelObj);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }


                            //循环赋值
                            for (V vo : voList) {
                                String getMethodOID = StringUtils.getGetMethodName(selfId);
                                Class<?> aClass = vo.getClass();
                                Method method = aClass.getMethod(getMethodOID);
                                //拿到每个对象的id
                                Object id = method.invoke(vo);
                                //如果id不为空
                                if (!ObjectUtils.isEmpty(id)) {
                                    if (!ObjectUtils.isEmpty(id)) {
                                        if (type.equals(VoExtendType.OBJECT)) {
                                            Object orDefaultValue = objMap.getOrDefault(id.toString(), null);
                                            voFiled.set(vo, orDefaultValue);
                                        } else if (type.equals(VoExtendType.Str)) {
                                            Object orDefaultValue = strMap.getOrDefault(id.toString(), null);
                                            voFiled.set(vo, orDefaultValue);
                                        }
                                    }
                                }}

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }


                }
            }
        }
        return voList;
    }

}




