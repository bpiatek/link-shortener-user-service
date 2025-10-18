package pl.bpiatek.linkshorteneruserservice.user;

record UserRegisteredApplicationEvent(
        String userId,
        String email,
        String verificationToken
) {}