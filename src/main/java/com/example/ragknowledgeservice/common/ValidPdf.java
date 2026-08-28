package com.example.ragknowledgeservice.common;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PdfFileValidator.class)
@Documented
public @interface ValidPdf {

    String message() default "Invalid PDF file";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}