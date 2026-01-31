package com.supabase.qnasession;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QnasessionApplication {

	public static void main(String[] args) {
		SpringApplication.run(QnasessionApplication.class, args);
	}

}
