package validation.arch;

public class DynaValidatorException extends Exception {
	private static final long serialVersionUID = 2663661378817142948L;

	public DynaValidatorException(String message, Throwable cause) {
		super(message, cause);
	}

	public DynaValidatorException(String message) {
		super(message);
	}

	public DynaValidatorException(Throwable cause) {
		super(cause);
	}

}
