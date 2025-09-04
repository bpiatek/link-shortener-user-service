package pl.bpiatek.linkshorteneruserservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestConfigController {

    private final String vaultTestMessage;

    public TestConfigController(@Value("${app.test.message}") String vaultTestMessage) {
        this.vaultTestMessage = vaultTestMessage;
    }

    @GetMapping("/test-config")
    public String getTestConfig() {
        return "The message from Vault is: '" + this.vaultTestMessage + "'";
    }
}