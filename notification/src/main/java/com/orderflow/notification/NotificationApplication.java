package com.orderflow.notification;

import com.orderflow.notification.infrastructure.BrevoProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.orderflow")
@ConfigurationPropertiesScan
public class NotificationApplication {
  public static void main(String[] args) {SpringApplication.run(NotificationApplication.class, args);}
}