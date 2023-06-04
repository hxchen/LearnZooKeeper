package com.example.vrgame;

import com.example.vrgame.zookeeper.ConfigCenter;
import org.springframework.context.ApplicationContext;

public class Application {
    private static ApplicationContext applicationContext;

    private static ConfigCenter configCenter;
    public static void setApplicationContext(ApplicationContext applicationContext){
        Application.applicationContext = applicationContext;
        configCenter = getBean(ConfigCenter.class);
    }
    public static ApplicationContext getApplicationContext(){
        return applicationContext;
    }

    public static void launch(){
        configCenter.launch();
    }
    public static <T> T getBean(Class<T> requiredType) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(requiredType);
    }
}
