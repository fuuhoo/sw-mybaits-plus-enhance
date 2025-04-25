package cn.siwei.fubin.swmybatisenhance.validator;

import cn.siwei.fubin.BaseException;

import cn.siwei.fubin.swmybatisenhance.annotation.ForeignkeyCheck;
import cn.siwei.fubin.swmybatisenhance.exception.MyDbException;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.util.ObjectUtils;
import cn.siwei.fubin.swmybatisenhance.util.SpringContextUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ForeignKeyValidator implements ConstraintValidator<ForeignkeyCheck, Object> {


    Class mapperClass;

    String idStr;

    Boolean canNull;

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {

        if (ObjectUtils.isEmpty(value)) {
            return true;
        }

        try {

            Method selectOne = mapperClass.getMethod("selectCount", Wrapper.class);
            QueryWrapper qwp = new QueryWrapper();
            ArrayList<Object> objects = new ArrayList<>();
            objects.add(value);
            qwp.in(idStr, objects);

            //可以为空的话空值返回true
            if (canNull) {
                if (ObjectUtils.isEmpty(value)) {
                    return true;
                }
            }

            Object bean = SpringContextUtils.getBean(mapperClass);
            Long count = (Long) selectOne.invoke(bean, qwp);
            if (count == 0) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new MyDbException("数据无效:"+e.getMessage());
        }
    }

    @Override
    public void initialize(ForeignkeyCheck constraintAnnotation) {
        mapperClass = constraintAnnotation.foreignMapperClass();
        idStr = constraintAnnotation.foreignId();
        canNull = constraintAnnotation.canNull();
    }
}
