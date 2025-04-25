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

    //必须得有
    Class<?>[] groups() default {};
//
    Class<? extends Payload>[] payload() default {};


}
