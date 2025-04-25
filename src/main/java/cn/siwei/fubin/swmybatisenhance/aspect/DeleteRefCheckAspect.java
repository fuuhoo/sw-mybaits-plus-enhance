package cn.siwei.fubin.swmybatisenhance.aspect;


import cn.siwei.fubin.swmybatisenhance.annotation.DeleteRefCheck;
import cn.siwei.fubin.swmybatisenhance.annotation.DeleteRefCheckList;
import cn.siwei.fubin.BaseException;

import cn.siwei.fubin.swmybatisenhance.helper.CheckRefHelper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Aspect
@Component
@Slf4j
public class DeleteRefCheckAspect {

    @Before(value = "@annotation(deleteRefCheck)")
    public void advice(JoinPoint joinPoint, DeleteRefCheck deleteRefCheck)  {
        check(joinPoint, deleteRefCheck);
    }


    @Before(value = "@annotation(deleteRefCheckList)")
    public void advice(JoinPoint joinPoint, DeleteRefCheckList deleteRefCheckList)  {
        DeleteRefCheck[] list = deleteRefCheckList.list();
        for (DeleteRefCheck deleteRefCheck : list) {
            check(joinPoint, deleteRefCheck);
        }

    }

    private void check(JoinPoint joinPoint, DeleteRefCheck deleteRefCheck) {

        String meaasge = deleteRefCheck.meaasge();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //mapper类
        Class aClass = deleteRefCheck.mapperClazz();
        //在引用里面的参数名称，mapper select的时候用
        String refFieldName = deleteRefCheck.refFieldName();
        //id的参数名称
        String selfId = deleteRefCheck.selfIdFieldName();
        //参数
        Object[] args = joinPoint.getArgs();
        //获取参数名称
        String[] parameterNames = signature.getParameterNames();

        Integer index = 0;
        for (String parameterName : parameterNames) {
            //参数等于传入的本身的id名称，一般为`id`
            if (parameterName.equals(selfId)) {
                CheckRefHelper checkRefHelper = new CheckRefHelper();
                Serializable arg =(Serializable) args[index];
                Boolean aBoolean = checkRefHelper.CheckRefByForeignID(aClass, refFieldName, arg);
                if (aBoolean){
                    throw  new BaseException(meaasge);
                }
            }
            index = index + 1;
        }

    }
}
