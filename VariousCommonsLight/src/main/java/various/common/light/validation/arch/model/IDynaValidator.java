package various.common.light.validation.arch.model;

import various.common.light.validation.arch.DynaValidatorException;

@FunctionalInterface
public interface IDynaValidator<T> {

	public boolean isValid(T value) throws DynaValidatorException;
}
