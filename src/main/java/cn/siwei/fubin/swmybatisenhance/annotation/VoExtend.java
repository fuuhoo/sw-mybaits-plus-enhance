package cn.siwei.fubin.swmybatisenhance.annotation;



import cn.siwei.fubin.swmybatisenhance.constant.VoExtendType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//自动填充外键的相关信息
//主要应用于多对一，例如，取学生所在班级的名称或者学生所在班级的班级信息
/**
 * @description: cn.siwei.fubin.swadmin.common.helper.ForeignKeyVoExtendUtil
 **/
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VoExtend {

    //mapper类
    Class<? extends BaseMapper> mapperName();

    //是否转驼峰
    boolean ifCamel2UnderLine() default false;

    //根据原始的哪个字段查
    String selfIdField() default "id";
    //拓展字段，要显示的名称
    String extNameField() default "name";


//    //数据库的字段名称
    String extDbNameField() default "";
    //拓展字段，在拓展对象的id
    String extIdField() default "id";
//    //数据库的字段名称
    String extDbIdField() default "";



    //拓展的类型。是一个字段还是一个对象。当type=OBJECT的时候是对象
    VoExtendType type() default VoExtendType.Str;


}
