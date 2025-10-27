package pl.bpiatek.linkshorteneruserservice.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import static java.nio.charset.StandardCharsets.*;

class JwtKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyProvider.class);
    private final KeyPair keyPair;

    JwtKeyProvider(String keyFilePath) {
        this.keyPair = loadRsaKeyPairFromVaultFile(keyFilePath);
    }

    RSAPrivateKey getPrivateKey() {
        return (RSAPrivateKey) keyPair.getPrivate();
    }

    RSAPublicKey getPublicKey() {
        return (RSAPublicKey) keyPair.getPublic();
    }

    private KeyPair loadRsaKeyPairFromVaultFile(String keyFilePath) {
        try {
            var mapper = new ObjectMapper();
            Map<String, String> keys = mapper.readValue(new File(keyFilePath), new TypeReference<>() {});

            var privateKeyB64 = keys.get("rsa-private-key");
            var publicKeyB64  = keys.get("rsa-public-key");

            // Step 1: Base64-decode Vault strings → PEM text
            var privatePem = new String(Base64.getDecoder().decode(privateKeyB64), UTF_8);
            var publicPem  = new String(Base64.getDecoder().decode(publicKeyB64), UTF_8);

            // Step 2: strip headers/footers + decode again
            var privateKey = decodePrivateKey(privatePem);
            var  publicKey  = decodePublicKey(publicPem);

            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA key pair from Vault", e);
        }
    }

    private PrivateKey decodePrivateKey(String pem) throws Exception {
        var normalized = pem
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        var der = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private PublicKey decodePublicKey(String pem) throws Exception {
        var normalized = pem
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        var der = Base64.getDecoder().decode(normalized);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }
}
