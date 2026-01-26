package cn.nuaa.jensonxu.fairy.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cn.nuaa.jensonxu")
@MapperScan("cn.nuaa.jensonxu.fairy.common.repository.mysql.mapper")
public class FairyWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(FairyWebApplication.class, args);
	}

}
