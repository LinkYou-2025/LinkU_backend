package com.umc.linkyou.validation.validator;

import com.umc.linkyou.validation.annotation.ValidEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValidator implements ConstraintValidator<ValidEnum, Object> {

    private Set<String> allowedNames;

    @Override
    public void initialize(ValidEnum annotation) {
        Set<String> enumNames = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toSet());

        String[] anyOf = annotation.anyOf();
        if (anyOf.length == 0) {
            this.allowedNames = enumNames;
            return;
        }

        for (String name : anyOf) {
            if (!enumNames.contains(name)) {
                throw new IllegalArgumentException(
                        "anyOf에 " + annotation.enumClass().getSimpleName() + "에 존재하지 않는 값이 지정되었습니다: " + name);
            }
        }
        this.allowedNames = Set.of(anyOf);
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String name = (value instanceof Enum<?> enumValue) ? enumValue.name() : value.toString();
        return allowedNames.contains(name);
    }
}
