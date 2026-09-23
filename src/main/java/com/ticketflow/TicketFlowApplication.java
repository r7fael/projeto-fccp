package com.ticketflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketFlowApplication.class, args);
	}

}
