package cz.aron.transfagent.peva;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import cz.aron.apux._2020.ItemDateRange;

public class Peva2UtilsTest {

	@Test
	public void testCorrectLineSeparators() {		
		assertEquals(Peva2Utils.correctLineSeparators("\rahoj"),"\r\nahoj");
		assertEquals(Peva2Utils.correctLineSeparators("ah\roj"),"ah\r\noj");
		assertEquals(Peva2Utils.correctLineSeparators("\rahoj\r"),"\r\nahoj\r\n");
		assertEquals(Peva2Utils.correctLineSeparators("\r\nahoj\r\n"),"\r\nahoj\r\n");
	}
	
	@Test
	public void testParseDating() {		
		compareIdr(Peva2Utils.parseDating("1.1.2000", "type"),"2000-01-01T00:00:00","2000-01-01T23:59:59","D-D");				
		compareIdr(Peva2Utils.parseDating("1928", "type"),"1928-01-01T00:00:00","1928-12-31T23:59:59","Y-Y");
		compareIdr(Peva2Utils.parseDating("1.2.1996", "type"),"1996-02-01T00:00:00","1996-02-01T23:59:59","D-D");
		compareIdr(Peva2Utils.parseDating("1990/2000", "type"),"1990-01-01T00:00:00","2000-12-31T23:59:59","Y-Y");
		compareIdr(Peva2Utils.parseDating("1.1.1990/2000", "type"),"1990-01-01T00:00:00","2000-12-31T23:59:59","D-Y");
		compareIdr(Peva2Utils.parseDating("1990/1.1.2000", "type"),"1990-01-01T00:00:00","2000-01-01T23:59:59","Y-D");
		compareIdr(Peva2Utils.parseDating("1.1.1990/1.1.2000", "type"),"1990-01-01T00:00:00","2000-01-01T23:59:59","D-D");
	}
	
	private void compareIdr(ItemDateRange idr, String f, String t, String fmt) {
		assertEquals(idr.getF(), f);
		assertEquals(idr.getTo(), t);
		assertEquals(idr.getFmt(), fmt);
	}
	
}
