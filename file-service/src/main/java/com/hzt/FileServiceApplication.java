package com.hzt;

import com.hzt.system.listener.ApplicationStartedEventListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class FileServiceApplication {
	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication();
		springApplication.addListeners(new ApplicationStartedEventListener());
		springApplication.run(FileServiceApplication.class, args);
	}
}
