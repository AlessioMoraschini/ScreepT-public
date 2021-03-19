package validator;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import validation.arch.DynaObjectValidator;
import validation.arch.DynaValidatorException;
import validation.arch.model.DynaChecker;

public class TestValidator {
	
	private static AtomicInteger validationsPassed = new AtomicInteger(0);
	private static AtomicInteger validationsFailed = new AtomicInteger(0);

	@Test
	@Ignore
	public void testValidation() throws DynaValidatorException {
		DynaObjectValidator<Integer> validator = getValidator();
		
		for (int i = 0; i < 10; i++) {
			try {
				int random = getRandomInt(1000);
				validator.validate(random);
				System.out.println(random + " is validated! :)");
			} catch (Exception e) {
				if (e instanceof DynaValidatorException) {
					System.out.println(e.getMessage());
				}
			}
		}
	}

//	@Test
//	@Ignore
	public static void main(String[] args) throws DynaValidatorException, InterruptedException {
		DynaObjectValidator<Integer> validator = getValidator();
		
		Thread a = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < 100; i++) {
					validate(validator, getRandomInt(1000));
//					try {
//						Thread.sleep(250);
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
				}
			}
		});

		Thread b = new Thread(() -> {
			for (int i = 0; i < 100; i++) {
				validate(validator, getRandomInt(1000));
//				try {
//					Thread.sleep(200);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
			}
		});
		
		a.start();
		b.start();
		
		a.join();
		b.join();
		
		int totalValidations = validationsFailed.get() + validationsPassed.get();
		
		System.out.println("\n\n#######################################################################################\n");
		System.out.println("Validations passed: " + validationsPassed + "/" + totalValidations);
		System.out.println("Validations failed: " + validationsFailed + "/" + totalValidations);
	}
	
	private static void validate(DynaObjectValidator<Integer> validator, int numberToValidate) {
		try {
			validator.validate(numberToValidate);
			System.out.println(numberToValidate + " is validated! :)");
			validationsPassed.incrementAndGet();
		} catch (Exception e) {
			if (e instanceof DynaValidatorException) {
				System.out.println(e.getMessage());
				validationsFailed.incrementAndGet();
			}
		}
	}
	
	private static int getRandomInt(int max) {
		Random rand = new Random();
		return rand.nextInt(max);
	}
	
	private static DynaObjectValidator<Integer> getValidator() {
		DynaObjectValidator<Integer> validator = new DynaObjectValidator<>(null);
		
		validator.addRuleChecker(new DynaChecker<Integer>(
			(i) -> {
				return i > 50;
			}, (i) -> {
				return "i=" + i + " is less than 300!";
			}));

		validator.addRuleChecker(new DynaChecker<Integer>(
			(i) -> {
				return i < 980;
			}, (i) -> {
				return "i=" + i + " is more than 900!";
			}));

		validator.addRuleChecker(new DynaChecker<Integer>(
				(i) -> {
					return i < 350;
				}, (i) -> {
					return "i=" + i + " is more than 800!";
				}));
		
		return validator;
	}
}
