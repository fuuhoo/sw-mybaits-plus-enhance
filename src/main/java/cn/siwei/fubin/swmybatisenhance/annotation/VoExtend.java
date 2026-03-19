package cn.siwei.fubin.swmybatisenhance.annotation;



import cn.siwei.fubin.swmybatisenhance.constant.VoExtendType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @description: cn.siwei.fubin.swadmin.common.helper.ForeignKeyVoExtendUtil
 * 自动填充外键的相关信息
 * 主要应用于多对一，例如，取学生所在班级的名称或者学生所在班级的班级信息
 **/
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VoExtend {

    //是否驼峰转下划线
    boolean ifCamel2UnderLine() default true;
    //外键的字段
    String foreignKeyForIdField() default "id";


    //外键模型的mapper类
    Class<? extends BaseMapper> mapperName();
    //数据库表的名称，可能不一致
    String extDbNameField() default "";
    //模型的id
    String extIdField() default "id";
    //数据库的id，可能和字段名不一致
    String extDbIdField() default "";
    //拓展字段，要显示的字段，一般为name
    String foreignModelFieldForName() default "name";


    //拓展的类型。是一个字段还是一个对象。当type=OBJECT的时候是对象
    VoExtendType type() default VoExtendType.Str;

}
