package app.dya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DyaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DyaBackendApplication.class, args);
	}

}
