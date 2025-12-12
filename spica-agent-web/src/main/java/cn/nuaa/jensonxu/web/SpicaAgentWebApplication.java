package cn.nuaa.jensonxu.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cn.nuaa.jensonxu")
@MapperScan("cn.nuaa.jensonxu.repository.mysql.mapper")
public class SpicaAgentWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpicaAgentWebApplication.class, args);
	}

}
