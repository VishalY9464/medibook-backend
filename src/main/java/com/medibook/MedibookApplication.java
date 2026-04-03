package com.medibook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedibookApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedibookApplication.class, args);
		System.out.println("Runninng......");
	}

}


//```
//
//**Why these annotations:**
//```
//@SpringBootApplication  → starts the entire Spring Boot app
//@EnableScheduling       → enables Quartz scheduler jobs we build later
//                          (slot expiry, reminders, no-show detection)