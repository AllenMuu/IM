package com.hzt.system.config;

import com.hzt.common.handler.HandlerExceptionResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description:
 * @Author:wangqinjun@vichain.com
 * @Date:2018/09/27 12:52
 */
@Configuration
public class GlobalExceptionConfig {

    @Bean
    public HandlerExceptionResolver getHandlerExceptionResolver(){
        return new HandlerExceptionResolver();
    }
}
