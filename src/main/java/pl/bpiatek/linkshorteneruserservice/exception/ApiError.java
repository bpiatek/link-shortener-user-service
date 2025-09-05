package pl.bpiatek.linkshorteneruserservice.exception;

import java.util.List;

record ApiError(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        List<ValidationError> errors
) {
}
