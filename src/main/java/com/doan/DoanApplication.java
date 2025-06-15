package com.doan;

import com.doan.model.Cache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class DoanApplication {

	public static void main(String[] args) {
		Cache.cache.initialize();
		SpringApplication.run(DoanApplication.class, args);
		startAutoSave();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			Cache.cache.saveToDisk();
		}));

	}

	public static void startAutoSave() {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(() -> {
			try {
				Cache.cache.saveToDisk();
			} catch (Exception e) {
				System.err.println("Auto-save failed: " + e.getMessage());
			}
		}, 1, 5, TimeUnit.MINUTES); // Save every 5 minutes, starting after 1 min
	}

}
