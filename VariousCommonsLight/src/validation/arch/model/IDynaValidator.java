package validation.arch.model;

import validation.arch.DynaValidatorException;

@FunctionalInterface
public interface IDynaValidator<T> {

	public boolean isValid(T value) throws DynaValidatorException;
}
