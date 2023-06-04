package com.example.vrgame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Validate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VrGameApplicationTests {
    @Value("${boyName}")
    private String boyName;

    @Value("${girlName}")
    private String girlName;

    @Test
    public void testZKConfig(){
        System.out.println("从Application配置中获取：boyName = " + boyName);
        System.out.println("从ZK配置中获取：girlName = " + girlName);
    }
}
