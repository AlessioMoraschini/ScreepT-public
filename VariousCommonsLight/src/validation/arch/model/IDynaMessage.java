package validation.arch.model;

@FunctionalInterface
public interface IDynaMessage<T> {

	public String getValidationErrorMessage(T objectToValidate);
}
