package cn.siwei.fubin.swmybatisenhance.annotation;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
* @Author fuhongbin
* @Description 限制删除，相当于各数据库的外键限制了了
* @Date 2024/12/20
  * @param null
*/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
/**
 * @description: 实现；类  package cn.siwei.fubin.swadmin.common.aspect.DeleteRefCheckAsept;
**/        
public @interface DeleteRefCheck {
    Class mapperClazz();
    String refFieldName();
    String selfIdFieldName() default "id";
    String meaasge() default "无法删除";
}
