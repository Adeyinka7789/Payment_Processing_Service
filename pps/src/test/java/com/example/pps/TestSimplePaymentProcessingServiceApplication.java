package com.example.pps;

import org.springframework.boot.SpringApplication;

public class TestSimplePaymentProcessingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(SimplePaymentProcessingServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
