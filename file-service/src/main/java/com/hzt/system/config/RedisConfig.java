package com.hzt.system.config;

import com.hzt.common.config.RedisConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * @Description:
 * @Author: wangqinjun@vichain.com
 * @Date: 2018/9/23 10:34
 */
@Configuration
@EnableCaching
public class RedisConfig extends RedisConfigurerSupport {

}
