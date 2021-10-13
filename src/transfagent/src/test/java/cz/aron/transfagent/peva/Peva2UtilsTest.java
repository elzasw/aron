package cz.aron.transfagent.peva;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class Peva2UtilsTest {

	@Test
	public void testCorrectLineSeparators() {		
		assertEquals(Peva2Utils.correctLineSeparators("\rahoj"),"\r\nahoj");
		assertEquals(Peva2Utils.correctLineSeparators("ah\roj"),"ah\r\noj");
		assertEquals(Peva2Utils.correctLineSeparators("\rahoj\r"),"\r\nahoj\r\n");
		assertEquals(Peva2Utils.correctLineSeparators("\r\nahoj\r\n"),"\r\nahoj\r\n");
	}
	
}
