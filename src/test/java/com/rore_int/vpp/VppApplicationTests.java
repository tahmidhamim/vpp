package com.rore_int.vpp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class VppApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads() {
		assertNotNull(context, "Application context should load");
	}
}
