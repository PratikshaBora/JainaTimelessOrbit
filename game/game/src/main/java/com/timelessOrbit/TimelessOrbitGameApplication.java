package com.timelessOrbit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling		// ✅ turn on scheduling
public class TimelessOrbitGameApplication {

	public static void main(String[] args) {
		SpringApplication.run(TimelessOrbitGameApplication.class, args);
	}

}
