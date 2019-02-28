package com.hzt.system.config;

import com.hzt.common.config.TransactionManager;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * @Description:
 * @Author:jiangjingsheng@vichain.com
 * @Date:2018/08/20 18:49
 */
@Configuration
public class TransactionConfig {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Bean
    public TransactionInterceptor txAdvice() {
        return TransactionManager.txAdvice(transactionManager);
    }

    @Bean
    public Advisor txAdviceAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(* com.hzt.file.*.service.impl.*.*(..))");
        return new DefaultPointcutAdvisor(pointcut, txAdvice());
    }
}
