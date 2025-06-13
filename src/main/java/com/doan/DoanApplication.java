package com.doan;

import com.doan.model.Cache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
public class DoanApplication {

	public static void main(String[] args) {
		Cache.cache.initialize();
		SpringApplication.run(DoanApplication.class, args);
	}

}
