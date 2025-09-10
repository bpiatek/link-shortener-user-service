package pl.bpiatek.linkshorteneruserservice.user;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final KeyPair keyPair;

    JwtKeyProvider(@Value("${vault.secrets.path:/vault/secrets/jwt-key-json}") String keyFilePath) {
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
            File keyFile = new File(keyFilePath);
            String jsonContent = new String(Files.readAllBytes(keyFile.toPath()));

            // 2. Parse the JSON into a Map
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> keys = mapper.readValue(jsonContent, Map.class);

            // 3. Decode the PEM strings into actual key objects
            PublicKey publicKey = decodePublicKey(keys.get("rsa-public-key"));
            PrivateKey privateKey = decodePrivateKey(keys.get("rsa-private-key"));

            return new KeyPair(publicKey, privateKey);

        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to load RSA key pair from Vault secret file: " + keyFilePath, e);
        }
    }

    // Helper method to parse the PEM-formatted private key
    private PrivateKey decodePrivateKey(String pemKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String privateKeyContent = pemKey
                .replaceAll("\\n", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
        return kf.generatePrivate(keySpecPKCS8);
    }

    // Helper method to parse the PEM-formatted public key
    private PublicKey decodePublicKey(String pemKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String publicKeyContent = pemKey
                .replaceAll("\\n", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
        return kf.generatePublic(keySpecX509);
    }
}
