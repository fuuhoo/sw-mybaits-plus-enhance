package cn.siwei.fubin.swmybatisenhance.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component  // 确保被 Spring 扫描并管理
public class SpringContextUtils implements ApplicationContextAware, ApplicationListener<ContextClosedEvent> {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext context) throws BeansException {
        applicationContext = context;   // 容器启动时自动注入
    }

    @Override
    public void onApplicationEvent(@NonNull ContextClosedEvent event) {
        applicationContext = null;     // 容器关闭时清空引用，避免后续调用
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static Object getBean(String beanName) {
        assertContextActive();
        return applicationContext.getBean(beanName);
    }

    public static <T> T getBean(Class<T> clazz) {
        assertContextActive();
        return applicationContext.getBean(clazz);
    }

    public static <T> T getBean(String name, Class<T> clazz) {
        assertContextActive();
        return applicationContext.getBean(name, clazz);
    }

    /**
     * 检查容器是否可用，不可用则抛出明确的业务异常（可替换为返回 Optional 等）
     */
    private static void assertContextActive() {
        if (applicationContext == null) {
            throw new IllegalStateException("Spring 容器未初始化或已被关闭");
        }
        // 可选：进一步检查容器是否处于 active 状态
        // if (!applicationContext.isActive()) { ... }
    }
}

