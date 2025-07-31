package cn.siwei.fubin.swmybatisenhance.initializer;

import cn.siwei.fubin.swmybatisenhance.util.SpringContextUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class MyApplicationContextInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        SpringContextUtils.setApplicationContext(applicationContext);
    }

}
