package cn.siwei.fubin.swmybatisenhance.helper;

import cn.siwei.fubin.BaseException;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Component;
import cn.siwei.fubin.swmybatisenhance.util.ClassNameConvertUtil;
import cn.siwei.fubin.swmybatisenhance.util.SpringContextUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;



@Component
public class CheckRefHelper {

    public Boolean CheckRefByForeignID(Class mapperClazz,  String fieldName, Serializable value)  {
        try {
            QueryWrapper tQueryWrapper = new QueryWrapper<>();

            //参数名称，转驼峰
            String fieldNameKey = ClassNameConvertUtil.toUnderScoreCase(fieldName);

            tQueryWrapper.eq(fieldNameKey, value);

            Method selectList = mapperClazz.getMethod("selectList", Wrapper.class);

            Object bean = SpringContextUtils.getBean(mapperClazz);

            List<Object> result = (List<Object>) selectList.invoke(bean, tQueryWrapper);

            if (result.size() > 0) {
                return true;
            } else {
                return false;
            }
        }catch (Exception e){
            throw new BaseException("数据执行失败:"+e.toString());
        }

    }
}
