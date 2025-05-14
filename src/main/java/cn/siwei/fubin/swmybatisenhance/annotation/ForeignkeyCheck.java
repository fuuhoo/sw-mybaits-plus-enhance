package cn.siwei.fubin.swmybatisenhance.annotation;





import cn.siwei.fubin.swmybatisenhance.validator.ForeignKeyValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = ForeignKeyValidator.class)
public @interface ForeignkeyCheck {


    Class foreignMapperClass();

    boolean canNull() default false;

    String foreignId() default "id";

    String message() default "数据不存在";


    /**
     * 必须包含以下两个属性
     * 否则会报错 error msg: contains Constraint annotation, but does not contain a groups parameter.
     *
     * @return
     */

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
