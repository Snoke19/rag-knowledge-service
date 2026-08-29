package com.example.ragknowledgeservice.common.validation;

import com.example.ragknowledgeservice.common.error.ValidationReason;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
public class PdfFileValidator implements ConstraintValidator<ValidPdf, MultipartFile> {

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

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

        if (!hasPdfSignature(file)) {
            return reject(context, ValidationReason.INVALID_PDF_CONTENT);
        }

        return true;
    }

    private boolean hasPdfSignature(MultipartFile file) {
        try(InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(PDF_SIGNATURE.length);
            return Arrays.equals(header, PDF_SIGNATURE);
        } catch (IOException exception) {
            log.warn("Could not read uploaded file to validate PDF signature", exception);
            return false;
        }
    }

    private boolean reject(ConstraintValidatorContext context, ValidationReason reason) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(reason.name()).addConstraintViolation();

        return false;
    }
}