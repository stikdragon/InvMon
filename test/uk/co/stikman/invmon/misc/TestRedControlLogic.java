package uk.co.stikman.invmon.misc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

import uk.co.stikman.invmon.controllers.RedControllerLogic;

public class TestRedControlLogic {
	@Test
	public void testStuff() {
		RedControllerLogic x = new RedControllerLogic(null);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		
		x.run(LocalDateTime.parse("2023-01-04 15:00"));
        

	}
}
