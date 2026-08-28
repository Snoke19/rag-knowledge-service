package com.example.ragknowledgeservice.common;

import com.example.ragknowledgeservice.common.error.ValidationReason;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
public class PdfFileValidator
    implements ConstraintValidator<ValidPdf, MultipartFile> {

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null) {
            return reject(context, ValidationReason.FILE_REQUIRED);
        }

        if (file.isEmpty()) {
            return reject(context, ValidationReason.EMPTY_FILE);
        }

        if (!MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())) {
            return reject(context, ValidationReason.UNSUPPORTED_FILE_TYPE);
        }

        return true;
    }

    private boolean reject(ConstraintValidatorContext context, ValidationReason reason) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(reason.name()).addConstraintViolation();

        return false;
    }
}