package dev.ase.teamproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This class contains the startup of the application.
 */
@SpringBootApplication
public final class Application {
  /** Prevent instantiation of utility class. */
  private Application() {
    // no-op
  }

  /**
   * Application entry point.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    SpringApplication.run(Application.class, args);
  }
}