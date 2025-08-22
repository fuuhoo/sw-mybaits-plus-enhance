package cn.siwei.fubin.swmybatisenhance.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

public class ReflectUtil {

    public static <T> T newInstanceIfPossible(Class<T> clazz) {
        if (clazz == null ||
                clazz.isInterface() ||
                clazz.isAnnotation() ||
                clazz.isArray() ||
                clazz.isEnum() ||
                clazz.isPrimitive() ||
                Modifier.isAbstract(clazz.getModifiers())) {
            return null; // 无法实例化的类型直接返回null
        }

        // 尝试无参构造器
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            makeAccessible(constructor);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            // 无参构造器不存在，继续尝试带参构造器
        } catch (Exception e) {
            return null; // 其他异常表明实例化失败
        }

        // 尝试带参构造器
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            try {
                makeAccessible(constructor);
                Class<?>[] paramTypes = constructor.getParameterTypes();
                Object[] args = new Object[paramTypes.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    args[i] = getDefaultValue(paramTypes[i]);
                }
                @SuppressWarnings("unchecked")
                T instance = (T) constructor.newInstance(args);
                return instance;
            } catch (Exception e) {
                // 当前构造器失败，继续尝试下一个
            }
        }
        return null; // 所有尝试均失败
    }

    // 设置构造器可访问（突破private限制）
    private static void makeAccessible(Constructor<?> constructor) {
        if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
    }

    // 获取类型的默认值
    private static Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        return null; // 对象类型或非基本类型
    }


}
