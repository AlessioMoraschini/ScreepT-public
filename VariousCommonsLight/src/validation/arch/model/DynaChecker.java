package validation.arch.model;

import validation.arch.DynaValidatorException;

public class DynaChecker<T> {
	
	public static final String VALIDATION_ERR_PREFIX = "Validation error -> ";

	private IDynaValidator<T> validatorHandler;
	private IDynaMessage<T> messageHandler;
	
	public DynaChecker(IDynaValidator<T> validator, IDynaMessage<T> message) {
		super();
		this.validatorHandler = validator;
		this.messageHandler = message;
	}
	
	public DynaChecker(IDynaValidator<T> validator, String errorMessage) {
		super();
		this.validatorHandler = validator;
		this.messageHandler = (object) -> {
			return VALIDATION_ERR_PREFIX + errorMessage;
		};
	}

	public IDynaValidator<T> getValidatorHandler() {
		return validatorHandler;
	}

	public void setValidatorHandler(IDynaValidator<T> validatorHandler) {
		this.validatorHandler = validatorHandler;
	}

	public IDynaMessage<T> getMessageHandler() {
		return messageHandler;
	}

	public void setMessageHandler(IDynaMessage<T> messageHandler) {
		this.messageHandler = messageHandler;
	}
	
	public void validate(T data) throws DynaValidatorException {
		if(!validatorHandler.isValid(data)) {
			throw new DynaValidatorException(messageHandler.getValidationErrorMessage(data));
		}
	}
}
