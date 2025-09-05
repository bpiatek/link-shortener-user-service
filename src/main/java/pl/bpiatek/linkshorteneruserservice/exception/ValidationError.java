package pl.bpiatek.linkshorteneruserservice.exception;

record ValidationError(
        String field,
        Object rejectedValue,
        String message
) {
}
