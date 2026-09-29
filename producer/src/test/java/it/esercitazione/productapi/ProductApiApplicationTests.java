package it.esercitazione.productapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductApiApplicationTests {

	@Test
	void applicationClassIsAvailable() {
		assertThat(ProductApiApplication.class).isNotNull();
	}

}
