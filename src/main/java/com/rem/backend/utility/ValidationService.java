package com.rem.backend.utility;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validation;

import java.util.Set;


public class ValidationService {


    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    public static <T> void validate(T object, String variableName) {

        if (object == null) {
            throw new IllegalArgumentException(variableName + " must not be null.");
        }else if(object == ""){
            throw new IllegalArgumentException(variableName + " must not be empty.");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(object);

        if (!violations.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder();
            for (ConstraintViolation<T> violation : violations) {
                errorMessage.append(variableName)
                        .append(" ")
                        .append(violation.getMessage()) // Appends only the error message
                        .append("\n");
            }
            throw new IllegalArgumentException(errorMessage.toString().trim());
        }
    }

}


