package validation.arch;

import java.util.ArrayList;
import java.util.List;

import validation.arch.model.DynaChecker;

public class DynaObjectValidator<T> {

	private List<DynaChecker<T>> dynaCheckers;
	
	private volatile boolean isValidating = false;

	public DynaObjectValidator(List<DynaChecker<T>> dynaValidators) {
		super();
		this.dynaCheckers = dynaValidators != null ? dynaValidators : new ArrayList<>();
	}
	
	public void addRuleChecker(DynaChecker<T> validator) {
		if(validator != null)
			this.dynaCheckers.add(validator);
	}
	
	public void validate(T value) throws DynaValidatorException {
		List<String> validationErrorMessages = new ArrayList<String>(); 
		
		if(dynaCheckers != null) {
			
			while (isValidating) {
				try {
					System.out.println("This validator is already validating data, I'm waiting...");
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			try {
				System.out.println("\nValidation started for object: " + value);
				isValidating = true;
				
				for (DynaChecker<T> dynaChecker : dynaCheckers) {
					try {
						dynaChecker.validate(value);
					} catch (DynaValidatorException e) {
						validationErrorMessages.add(e.getMessage());
					}
				} 
			
			} finally {
				System.out.println("Validation completed with " + validationErrorMessages.size() + " errors!");
				isValidating = false;
			}
		}
		
		if(!validationErrorMessages.isEmpty()) {
			throw new DynaValidatorException(getMessagesConcatenation(validationErrorMessages));
		}
	}
	
	private String getMessagesConcatenation(List<String> messages) {
		String concatenated = "";
		
		for(String message : messages) {
			concatenated += message + "\n";
		}
		
		return concatenated;
	}

	public List<DynaChecker<T>> getDynaChekers() {
		return dynaCheckers;
	}

	public void setDynaChekers(List<DynaChecker<T>> dynaChekers) {
		this.dynaCheckers = dynaChekers;
	}

	public void resetDynaChekers() {
		this.dynaCheckers = new ArrayList<DynaChecker<T>>();
	}
	
}
