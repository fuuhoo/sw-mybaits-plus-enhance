package cn.siwei.fubin.swmybatisenhance.handler;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.ParameterizedType;
import java.util.List;

/**
 * 通用 List 集合类型处理器
 */
public abstract class BaseListTypeHandler<T> extends JacksonTypeHandler {

    // 1. 直接定义一个静态的 ObjectMapper，或者从 Spring 容器获取
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final JavaType javaType;

    public BaseListTypeHandler(Class<?> type) {
        super(type);
        // 2. 依然通过反射获取子类定义的泛型 T
        Class<T> clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        // 3. 构造出 List<T> 的具体类型信息
        this.javaType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
    }

    // 4. 只重写 parse 方法（这是 JacksonTypeHandler 暴露出来的唯一解析入口）
    @Override
    protected Object parse(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (Exception e) {
            throw new RuntimeException("JSON反序列化失败: " + json, e);
        }
    }

    // 注意：toJson 方法通常不需要重写，因为序列化 List 并不需要泛型信息
}
