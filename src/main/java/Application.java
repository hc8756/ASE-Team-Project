import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This class contains the startup of the application.
 */
@SpringBootApplication(scanBasePackages = {"controller","service","model"})
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
