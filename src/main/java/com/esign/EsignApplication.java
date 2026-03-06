package com.esign;

import java.util.Collections;

import javax.annotation.Resource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.esign.services.FilesStorageService;
import com.esign.services.StorageService;
/*
@EnableAutoConfiguration(exclude = {ErrorMvcAutoConfiguration.class})
@EnableJpaRepositories(basePackages= {"com.esign.repository"})
@EnableTransactionManagement
@EntityScan(basePackages= {"com.esign.model", "com.esign.model.AgreementAdobe", "com.esign.AgreementAdobe.Responses", "com.esign.model.AgreementAdobe.Webhook", "com.esign.AgreementRequest"})
@ComponentScan(basePackages = "com.esign")
*/ 
//@SpringBootApplication
@EnableScheduling
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class EsignApplication {

	@Resource
	StorageService storageService;
	public static void main(String[] args) {
		//SpringApplication.run(EsignApplication.class, args);
        SpringApplication app = new SpringApplication(EsignApplication.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "9090"));
        app.run(args); 	
	}
	
	@Bean
	CommandLineRunner init(FilesStorageService storageService) {
		return (args) -> { storageService.deleteAll();
	    storageService.init();
		};
	}

	   @Bean
	    public PasswordEncoder getPasswordEncoder() {
	        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	        return encoder;
	    }
}