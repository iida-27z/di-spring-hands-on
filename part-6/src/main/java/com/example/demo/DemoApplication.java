package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
		// DIコンテナによる自動配線が行われるので、明示的にオブジェクトを生成する必要はない。
		// CommandLineRunner を実装したコンポーネントは、起動時に自動的に実行されるので、明示的に呼び出す必要はない。
	}

}
