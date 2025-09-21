package cn.nuaa.jensonxu.gensokyo.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cn.nuaa.jensonxu.gensokyo")
@MapperScan("cn.nuaa.jensonxu.gensokyo.repository.mysql.mapper")
public class GensokyoWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(GensokyoWebApplication.class, args);
	}

}
