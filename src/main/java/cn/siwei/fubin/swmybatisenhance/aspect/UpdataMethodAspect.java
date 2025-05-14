package cn.siwei.fubin.swmybatisenhance.aspect;

import cn.siwei.fubin.BaseException;
import cn.siwei.fubin.swmybatisenhance.annotation.Updatable;
import cn.siwei.fubin.swmybatisenhance.annotation.UpdateMethod;
import cn.siwei.fubin.swmybatisenhance.exception.MyDbException;
import cn.siwei.fubin.swmybatisenhance.util.StringUtils;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;


@Aspect
@Component
@Slf4j
public class UpdataMethodAspect {

    @Before(value = "@annotation(updateMethod)")
    public void advice(JoinPoint joinPoint, UpdateMethod updateMethod) {

        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        Class paramEntityClass = updateMethod.modelClazz();

        Integer argIndex = 0;
        //获取被注解类的参数类型
        Class[] parameterTypes = signature.getParameterTypes();
        for (Class parameterType : parameterTypes) {
            //只处理注解中出现的
            Object arg = args[argIndex];
            if (paramEntityClass.equals(parameterType)) {
                //参数类型
                Field[] fields = parameterType.getDeclaredFields();
                for (Field field : fields) {
                    TableId idAnno = field.getAnnotation(TableId.class);
                    Updatable updateAnno = field.getAnnotation(Updatable.class);
                    boolean allowUpdate=false;
                    if(!ObjectUtils.isEmpty(updateAnno)){
                        allowUpdate = updateAnno.allowedUpdate();
                    }

                    if (!ObjectUtils.isEmpty(idAnno)) {
                        Object invoke;
                        try {
                            Method getmethod = parameterType.getMethod(StringUtils.getGetMethodName(field.getName()));
                            invoke= getmethod.invoke(arg);

                        }catch (Exception e){
                            e.printStackTrace();
                            throw new BaseException("发生内部错误,更新失败");
                        }
                        if(ObjectUtils.isEmpty(invoke)){
                            throw  new BaseException("id不可为空");
                        }
                    }
                    //传了不可更新的字段，由原来的抛出异常修改为不做任何处理


                    //没有这个注解，且不是id，全部修改为null
                    if (ObjectUtils.isEmpty(updateAnno)&& ObjectUtils.isEmpty(idAnno)) {
                        Object invoke;
//                        Method[] methods = parameterType.getMethods();
                        try {
                            Method getmethod = parameterType.getMethod(StringUtils.getGetMethodName(field.getName()));
                            Method setmethod = parameterType.getMethod(StringUtils.getSetMethodName(field.getName()),field.getType());

                            invoke= getmethod.invoke(arg);

                            if (!ObjectUtils.isEmpty(invoke)) {
                                if(!allowUpdate){
                                    throw new MyDbException("字段禁止更新:"+idAnno.value());
                                }else {
                                    Object invoke1 = setmethod.invoke(arg, new Object[]{null});
                                }

                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new MyDbException("获取值失败,更新失败");
                        }

                    }
                }
            }
            argIndex = argIndex + 1;
        }


    }


}
