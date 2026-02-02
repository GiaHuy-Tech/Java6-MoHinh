	package com.example.demo;

	import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
	@EnableScheduling // 2. Thêm dòng này để cho phép chạy tác vụ ngầm
	@SpringBootApplication
	public class ExampleApplication {

		public static void main(String[] args) {
			SpringApplication.run(ExampleApplication.class, args);
		}

	}
