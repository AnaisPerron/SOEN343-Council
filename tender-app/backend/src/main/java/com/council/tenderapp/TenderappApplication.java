package com.council.tenderapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TenderappApplication {

	public static void main(String[] args) {
		System.out.println("Working Directory: " + System.getProperty("user.dir"));
		System.out.println("DB_URL: " + System.getenv("DB_URL"));
		System.out.println("??????????????????DB_URL: " + System.getenv("DB_URL"));
		SpringApplication.run(TenderappApplication.class, args);
	}

}
