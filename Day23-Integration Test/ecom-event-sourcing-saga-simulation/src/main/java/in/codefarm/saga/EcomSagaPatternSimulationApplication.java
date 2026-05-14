package in.codefarm.saga;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EcomSagaPatternSimulationApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcomSagaPatternSimulationApplication.class, args);
	}

}

