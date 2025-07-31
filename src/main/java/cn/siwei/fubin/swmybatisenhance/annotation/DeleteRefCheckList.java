package cn.siwei.fubin.swmybatisenhance.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @Author fuhongbin
 * @Description 存在禁止删除，相当于各数据库的外键限制了了
 * @Date 2024/12/20
 * @param null
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DeleteRefCheckList {

    DeleteRefCheck[] list();

}
