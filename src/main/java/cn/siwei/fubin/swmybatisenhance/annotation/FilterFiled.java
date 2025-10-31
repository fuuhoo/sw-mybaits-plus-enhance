package cn.siwei.fubin.swmybatisenhance.annotation;




import cn.siwei.fubin.swmybatisenhance.constant.FilterTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterFiled {
    FilterTypeEnum type() default FilterTypeEnum.EQ;
    String listField() default "";
    String jsonObjName() default "";
    String aliasName() default "";
    //是否驼峰转下划线
    boolean ifCamel2UnderLine() default true;
}
