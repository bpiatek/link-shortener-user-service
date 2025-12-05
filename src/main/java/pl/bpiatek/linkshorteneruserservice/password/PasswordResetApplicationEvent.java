package pl.bpiatek.linkshorteneruserservice.password;

record PasswordResetApplicationEvent(
        String userId,
        String email,
        String resetUrl) {
}
