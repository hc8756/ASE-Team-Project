package dev.ase.teamproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This class contains the startup of the application.
 */
@SpringBootApplication
public final class Application {
  private Application() {
  }

  /**
   * Launches the Spring Boot application.
   *
   * @param args Command line arguments passed to the application.
   */
  public static void main(final String[] args) {
    SpringApplication.run(Application.class, args);
  }
}