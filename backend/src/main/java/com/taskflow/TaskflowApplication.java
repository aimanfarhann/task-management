package com.taskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point of the TaskFlow REST API. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class TaskflowApplication {

  /**
   * Boots the Spring application context.
   *
   * @param args standard command line arguments, passed through to Spring
   */
  public static void main(String[] args) {
    SpringApplication.run(TaskflowApplication.class, args);
  }
}
