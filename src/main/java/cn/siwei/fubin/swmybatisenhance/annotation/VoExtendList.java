package cn.siwei.fubin.swmybatisenhance.annotation;

import cn.siwei.fubin.swmybatisenhance.constant.VoExtendType;
import cn.siwei.fubin.swmybatisenhance.mapper.BaseMapperPlus;
import cn.siwei.fubin.swmybatisenhance.mapper.EmptyMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @description: cn.siwei.fubin.swadmin.common.helper.ForeignKeyVoExtendUtil
 * 主要用于多对多或者一对多
 * 自动填充反向外键的相关信息
 * 主要应用于一对多。例如取班级内部所有的学生的姓名或者学生的相关信息
 **/
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VoExtendList {
    //mapper类
    Class<? extends BaseMapper> extMapper();
    //关联关系表mapper类
    Class<? extends BaseMapper> relationMapper() default EmptyMapper.class;
    //是否驼峰转下划线
    boolean ifCamel2UnderLine() default true;
    //关联关系自身id
    String relationSelfId() default "";
    //关联关系拓展id
    String relationExtId() default  "";
    //根据原始的字段id
    String selfModelIdField() default "id";
    //根据原始的字段id
    String extModelIdField() default "id";
    //拓展的类型。是一个字段还是一个对象。当type=OBJECT的时候是对象
    VoExtendType type() default VoExtendType.Str;
    //拓展字段，要显示的名称
    String extNameField() default "name";
    //反向外键的拓展字段
    String selfIdInExtModel() default "id";
}