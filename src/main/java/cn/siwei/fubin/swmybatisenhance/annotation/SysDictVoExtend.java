package cn.siwei.fubin.swmybatisenhance.annotation;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SysDictVoExtend {

    //字典的数据库的mapper
    Class<? extends BaseMapper>  mapperName() ;
    //原数据的id字段名称
    String selfModelFileName() ;
    //对象的字典类型的名称
    String sysDictTypeFiedName();
    //字典类型
    String sysDictTypeName() default  "dic_type";
    //要显示的字段的名称
    String sysDictValueFiedName() default "label";
    //字典库的对应id字段
    String sysDictKeyFiedName() default "code";
}
