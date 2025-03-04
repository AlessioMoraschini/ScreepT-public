
import org.junit.Test;

import puppynoid.om.Bonus;

public class BonusTest {

	@Test
	public void testRandomBonus() {
		for(int i = 0; i < 100; i++) {
			System.out.println(Bonus.getRandomBonus((float)i));
		}
	}
}
