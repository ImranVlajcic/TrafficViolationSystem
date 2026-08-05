package com.academy.trafficviolationsystem.core.exceptions.infrastructure;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import org.springframework.http.HttpStatus;

public class PdfGenerationException extends AppException {
    public PdfGenerationException(String detail) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.PDF_GENERATION_ERROR,
                "PDF generation failed: " + detail);
    }
}
