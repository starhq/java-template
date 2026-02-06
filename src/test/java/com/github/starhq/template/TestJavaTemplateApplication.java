package com.github.starhq.template;

import org.springframework.boot.SpringApplication;

public class TestJavaTemplateApplication {

	public static void main(String[] args) {
		SpringApplication.from(JavaTemplateApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
