package pl.bpiatek.linkshorteneruserservice.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

@Component
class JwtKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyProvider.class);
    private final KeyPair keyPair;

    JwtKeyProvider(@Value("${vault.secrets.path:/vault/secrets/jwtkeyjson}") String keyFilePath) {
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
            // 1. Read the JSON file injected by the Vault Agent
            var keyFile = new File(keyFilePath);
            var jsonContent = new String(Files.readAllBytes(keyFile.toPath()));

            // 2. Parse the JSON into a Map
            var mapper = new ObjectMapper();
            Map<String, String> keys = mapper.readValue(jsonContent, Map.class);
            log.info("Keys loaded from Vault: {}", keys);
            if(keys.get("rsa-public-key") != null) {
                log.info("RSA public key: {}", keys.get("rsa-public-key"));
            }

            if(keys.get("rsa-private-key") != null) {
                log.info("RSA private key: {}", keys.get("rsa-private-key"));
            }

            // 3. Decode the PEM strings into actual key objects
            var publicKey = decodePublicKey(keys.get("rsa-public-key"));
            var privateKey = decodePrivateKey(keys.get("rsa-private-key"));

            return new KeyPair(publicKey, privateKey);

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to load RSA key pair from Vault secret file: " + keyFilePath, e);
        }
    }

    // Helper method to parse the PEM-formatted private key
    private PrivateKey decodePrivateKey(String base64Key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        var keyBytes = Base64.getDecoder().decode(base64Key.trim());
        var keySpec = new PKCS8EncodedKeySpec(keyBytes);
        var kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

    // Helper method to decode a Base64 string into a PublicKey
    private PublicKey decodePublicKey(String base64Key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        var keyBytes = Base64.getDecoder().decode(base64Key.trim());
        var keySpec = new X509EncodedKeySpec(keyBytes);
        var kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(keySpec);
    }
}
