package ru.agroeco.pulse.videoserver.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;

@RestControllerAdvice(basePackages = "ru.agroeco.pulse.videoserver")
public class VideoServerExceptionHandler {

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail onBusinessRule(BusinessRuleException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT); // ← было UNPROCESSABLE_ENTITY
        pd.setTitle("Business rule violated");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail onIntegrity(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Duplicate event");
        pd.setDetail("An event with the same externalEventId already exists. Retry to fetch it.");
        return pd;
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ProblemDetail onInvalidDataAccess(InvalidDataAccessApiUsageException ex) {
        // это ошибка клиента (невалидный sort/query param), не 500
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Invalid query parameter");
        pd.setDetail(ex.getMostSpecificCause().getMessage());
        return pd;
    }

}