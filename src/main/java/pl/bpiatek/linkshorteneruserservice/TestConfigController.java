package pl.bpiatek.linkshorteneruserservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestConfigController {

    private static final Logger log = LoggerFactory.getLogger(TestConfigController.class);

    private final String vaultTestMessage;

    public TestConfigController(@Value("${app.test.message}") String vaultTestMessage) {
        this.vaultTestMessage = vaultTestMessage;
    }

    @GetMapping("/test-config")
    public String getTestConfig(
            @RequestHeader(name = "X-User-Id", required = false) String userId,
            @RequestHeader(name = "X-User-Role", required = false) String userRole
    ) {

        log.info("Request received for /test-config");
        log.info("Header X-User-Id: {}", userId);
        log.info("Header X-User-Role: {}", userRole);

        return "The message from Vault is: '" + this.vaultTestMessage + "'";
    }
}