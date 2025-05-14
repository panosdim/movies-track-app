package eu.deltasw.auth_service;

import eu.deltasw.common.exception.autoconfigure.ExceptionHandlerAutoConfiguration;
import eu.deltasw.common.security.autoconfigure.JwtAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({JwtAutoConfiguration.class, ExceptionHandlerAutoConfiguration.class})
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
