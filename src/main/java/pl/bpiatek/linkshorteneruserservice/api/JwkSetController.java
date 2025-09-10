package pl.bpiatek.linkshorteneruserservice.api;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.bpiatek.linkshorteneruserservice.user.UserFacade;

import java.util.Map;

@RestController
class JwkSetController {

    private final UserFacade userFacade;

    JwkSetController(UserFacade userFacade) {
        this.userFacade = userFacade;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        var rsaKey = new RSAKey.Builder(userFacade.getPublicKey())
                .keyID("rsa-key-1")
                .build();

        var jwkSet = new JWKSet(rsaKey);

        return jwkSet.toJSONObject();
    }
}
