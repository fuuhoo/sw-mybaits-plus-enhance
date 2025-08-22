package cn.siwei.fubin.swmybatisenhance.handler;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.io.IOException;
import java.util.List;

@Slf4j
@MappedTypes({Object.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class JacksonListTypeHandler  extends AbstractJsonTypeHandler<Object> {

    private static ObjectMapper OBJECT_MAPPER;
    private final Class<?> type;

    public JacksonListTypeHandler(Class<?> type) {
        if (log.isTraceEnabled()) {
            log.trace("JacksonTypeHandler(" + type + ")");
        }
        Assert.notNull(type, "Type argument cannot be null");
        this.type = type;
    }


    @Override
    protected Object parse(String json) {
        try {
            return getObjectMapper().readValue(json, OBJECT_MAPPER.getTypeFactory().constructParametricType(List.class, type));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String toJson(Object obj) {
        try {
            return getObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectMapper getObjectMapper() {
        if (null == OBJECT_MAPPER) {
            OBJECT_MAPPER = new ObjectMapper();
        }
        return OBJECT_MAPPER;
    }

    public static void setObjectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper should not be null");
        JacksonListTypeHandler.OBJECT_MAPPER = objectMapper;
    }
}
