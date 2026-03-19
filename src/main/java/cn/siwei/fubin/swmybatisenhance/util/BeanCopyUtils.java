package cn.siwei.fubin.swmybatisenhance.util;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * bean拷贝工具(不使用cglib，带缓存优化)
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanCopyUtils {

    /**
     * 属性映射缓存，存储源类和目标类之间的属性对应关系
     */
    private static final Map<String, List<PropertyMapping>> PROPERTY_MAPPING_CACHE = new ConcurrentHashMap<>();

    /**
     * BeanCopier缓存（替代CGLib的BeanCopierCache）
     * 使用我们自己的实现来模拟BeanCopier的功能
     */
    public enum BeanCopierCache {
        /**
         * BeanCopier属性缓存单例
         */
        INSTANCE;

        /**
         * 获得类对应的属性映射
         *
         * @param srcClass    源Bean的类
         * @param targetClass 目标Bean的类
         * @param converter   转换器（暂不支持，保留参数）
         * @return 虚拟的BeanCopier对象（实际上是我们自己实现的拷贝逻辑）
         */
        public BeanCopier get(Class<?> srcClass, Class<?> targetClass, Object converter) {
            // 这里返回一个虚拟的BeanCopier，实际上调用我们自己的拷贝逻辑
            return new BeanCopier() {
                @Override
                public void copy(Object source, Object target, Object converter) {
                    // 直接调用我们的拷贝方法
                    copyInternal(source, target);
                }
            };
        }

        /**
         * 内部拷贝实现
         */
        private void copyInternal(Object source, Object target) {
            if (ObjectUtils.isNull(source) || ObjectUtils.isNull(target)) {
                return;
            }

            List<PropertyMapping> mappings = getPropertyMappings(source.getClass(), target.getClass());

            for (PropertyMapping mapping : mappings) {
                try {
                    Object value = mapping.getter.invoke(source);
                    mapping.setter.invoke(target, value);
                } catch (Exception e) {
                    // 忽略无法读取或设置的属性
                }
            }
        }
    }

    /**
     * 虚拟的BeanCopier接口
     */
    public interface BeanCopier {
        void copy(Object source, Object target, Object converter);
    }

    /**
     * 单对象基于class创建拷贝
     *
     * @param source 数据来源实体
     * @param desc   描述对象 转换后的对象
     * @return desc
     */
    public static <T, V> V copy(T source, Class<V> desc) {
        if (ObjectUtils.isNull(source)) {
            return null;
        }
        if (ObjectUtils.isNull(desc)) {
            return null;
        }
        try {
            final V target = desc.getDeclaredConstructor().newInstance();
            return copy(source, target);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + desc.getName(), e);
        }
    }

    /**
     * 单对象基于对象创建拷贝
     *
     * @param source 数据来源实体
     * @param desc   转换后的对象
     * @return desc
     */
    public static <T, V> V copy(T source, V desc) {
        if (ObjectUtils.isNull(source)) {
            return null;
        }
        if (ObjectUtils.isNull(desc)) {
            return null;
        }

        // 使用BeanCopierCache获取"BeanCopier"并执行拷贝
        BeanCopier beanCopier = BeanCopierCache.INSTANCE.get(source.getClass(), desc.getClass(), null);
        beanCopier.copy(source, desc, null);
        return desc;
    }

    /**
     * 获取属性映射关系（带缓存）
     */
    private static List<PropertyMapping> getPropertyMappings(Class<?> sourceClass, Class<?> targetClass) {
        String cacheKey = sourceClass.getName() + "_" + targetClass.getName();

        return PROPERTY_MAPPING_CACHE.computeIfAbsent(cacheKey, k -> {
            List<PropertyMapping> mappings = new ArrayList<>();

            // 获取源类的getter方法
            Map<String, Method> sourceGetters = getGetters(sourceClass);
            // 获取目标类的setter方法
            Map<String, Method> targetSetters = getSetters(targetClass);

            // 匹配同名属性
            for (Map.Entry<String, Method> entry : sourceGetters.entrySet()) {
                String propertyName = entry.getKey();
                Method getter = entry.getValue();
                Method setter = targetSetters.get(propertyName);

                if (setter != null) {
                    // 检查类型是否兼容
                    Class<?> getterReturnType = getter.getReturnType();
                    Class<?> setterParameterType = setter.getParameterTypes()[0];

                    if (isTypeCompatible(getterReturnType, setterParameterType)) {
                        mappings.add(new PropertyMapping(getter, setter));
                    }
                }
            }

            return mappings;
        });
    }

    /**
     * 获取类的getter方法映射
     */
    private static Map<String, Method> getGetters(Class<?> clazz) {
        Map<String, Method> getters = new HashMap<>();
        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            if (isGetter(method)) {
                String propertyName = getPropertyNameFromGetter(method);
                if (propertyName != null) {
                    getters.put(propertyName, method);
                }
            }
        }
        return getters;
    }

    /**
     * 获取类的setter方法映射
     */
    private static Map<String, Method> getSetters(Class<?> clazz) {
        Map<String, Method> setters = new HashMap<>();
        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            if (isSetter(method)) {
                String propertyName = getPropertyNameFromSetter(method);
                if (propertyName != null) {
                    setters.put(propertyName, method);
                }
            }
        }
        return setters;
    }

    /**
     * 判断方法是否为getter
     */
    private static boolean isGetter(Method method) {
        return (method.getName().startsWith("get") && method.getParameterCount() == 0 &&
                !void.class.equals(method.getReturnType()))
                || (method.getName().startsWith("is") && method.getParameterCount() == 0 &&
                (boolean.class.equals(method.getReturnType()) || Boolean.class.equals(method.getReturnType())));
    }

    /**
     * 判断方法是否为setter
     */
    private static boolean isSetter(Method method) {
        return method.getName().startsWith("set") && method.getParameterCount() == 1;
    }

    /**
     * 从getter方法获取属性名
     */
    private static String getPropertyNameFromGetter(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        } else if (name.startsWith("is") && name.length() > 2) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return null;
    }

    /**
     * 从setter方法获取属性名
     */
    private static String getPropertyNameFromSetter(Method method) {
        String name = method.getName();
        if (name.startsWith("set") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        return null;
    }

    /**
     * 判断类型是否兼容
     */
    private static boolean isTypeCompatible(Class<?> sourceType, Class<?> targetType) {
        // 相同类型
        if (sourceType.equals(targetType)) {
            return true;
        }

        // 基本类型和包装类型的兼容性
        if ((sourceType.isPrimitive() && getWrapperClass(sourceType).equals(targetType)) ||
                (targetType.isPrimitive() && getWrapperClass(targetType).equals(sourceType))) {
            return true;
        }

        // 子类可以赋值给父类
        if (targetType.isAssignableFrom(sourceType)) {
            return true;
        }

        return false;
    }

    /**
     * 获取基本类型的包装类
     */
    private static Class<?> getWrapperClass(Class<?> primitiveClass) {
        if (boolean.class.equals(primitiveClass)) return Boolean.class;
        if (byte.class.equals(primitiveClass)) return Byte.class;
        if (char.class.equals(primitiveClass)) return Character.class;
        if (short.class.equals(primitiveClass)) return Short.class;
        if (int.class.equals(primitiveClass)) return Integer.class;
        if (long.class.equals(primitiveClass)) return Long.class;
        if (float.class.equals(primitiveClass)) return Float.class;
        if (double.class.equals(primitiveClass)) return Double.class;
        if (void.class.equals(primitiveClass)) return Void.class;
        return primitiveClass;
    }

    /**
     * 属性映射关系内部类
     */
    private static class PropertyMapping {
        private final Method getter;
        private final Method setter;

        public PropertyMapping(Method getter, Method setter) {
            this.getter = getter;
            this.setter = setter;
        }

        public Method getGetter() {
            return getter;
        }

        public Method getSetter() {
            return setter;
        }
    }

    // 以下是其他方法（保持不变）

    /**
     * 列表对象基于class创建拷贝
     *
     * @param sourceList 数据来源实体列表
     * @param desc       描述对象 转换后的对象
     * @return desc
     */
    public static <T, V> List<V> copyList(List<T> sourceList, Class<V> desc) {
        if (ObjectUtils.isNull(sourceList)) {
            return null;
        }
        if (ObjectUtils.isEmpty(sourceList)) {
            return Collections.emptyList();
        }

        List<V> result = new ArrayList<>(sourceList.size());
        for (T source : sourceList) {
            if (source != null) {
                result.add(copy(source, desc));
            }
        }
        return result;
    }

    /**
     * bean拷贝到map
     *
     * @param bean 数据来源实体
     * @return map对象
     */
    public static <T> Map<String, Object> copyToMap(T bean) {
        if (ObjectUtils.isNull(bean)) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        Method[] methods = bean.getClass().getMethods();

        for (Method method : methods) {
            if (isGetter(method)) {
                String propertyName = getPropertyNameFromGetter(method);
                if (propertyName != null) {
                    try {
                        Object value = method.invoke(bean);
                        map.put(propertyName, value);
                    } catch (Exception e) {
                        // 忽略无法读取的属性
                    }
                }
            }
        }
        return map;
    }

    /**
     * map拷贝到bean
     *
     * @param map       数据来源
     * @param beanClass bean类
     * @return bean对象
     */
    public static <T> T mapToBean(Map<String, Object> map, Class<T> beanClass) {
        if (ObjectUtils.isEmpty(map)) {
            return null;
        }
        if (ObjectUtils.isNull(beanClass)) {
            return null;
        }
        try {
            T bean = beanClass.getDeclaredConstructor().newInstance();
            return mapToBean(map, bean);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + beanClass.getName(), e);
        }
    }

    /**
     * map拷贝到bean
     *
     * @param map  数据来源
     * @param bean bean对象
     * @return bean对象
     */
    public static <T> T mapToBean(Map<String, Object> map, T bean) {
        if (ObjectUtils.isEmpty(map)) {
            return null;
        }
        if (ObjectUtils.isNull(bean)) {
            return null;
        }

        Map<String, Method> setters = getSetters(bean.getClass());

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Method setter = setters.get(entry.getKey());
            if (setter != null) {
                try {
                    setter.invoke(bean, entry.getValue());
                } catch (Exception e) {
                    // 类型不匹配或其他异常，跳过此属性
                }
            }
        }
        return bean;
    }

    /**
     * map拷贝到map
     *
     * @param map   数据来源
     * @param clazz 返回的对象类型
     * @return map对象
     */
    public static <T, V> Map<String, V> mapToMap(Map<String, T> map, Class<V> clazz) {
        if (ObjectUtils.isEmpty(map)) {
            return null;
        }
        if (ObjectUtils.isNull(clazz)) {
            return null;
        }

        Map<String, V> copyMap = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, T> entry : map.entrySet()) {
            copyMap.put(entry.getKey(), copy(entry.getValue(), clazz));
        }
        return copyMap;
    }
}