package pl.bpiatek.linkshorteneruserservice.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtKeyProviderTest {

    @TempDir
    private Path tempDir;

    private static KeyPair sourceKeyPair;

    @BeforeAll
    static void generateSourceKeyPair() throws NoSuchAlgorithmException {
        var kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        sourceKeyPair = kpg.generateKeyPair();
    }

    @Test
    void shouldLoadKeyPairSuccessfullyFromValidFile() throws IOException {
        // given
        var keyFile = createTestKeyFile(tempDir, sourceKeyPair);

        // when
        var keyProvider = new JwtKeyProvider(keyFile.getAbsolutePath());

        // then
        var publicKey = keyProvider.getPublicKey();
        var privateKey = keyProvider.getPrivateKey();

        assertThat(publicKey).isNotNull();
        assertThat(privateKey).isNotNull();
        assertThat(publicKey).isEqualTo(sourceKeyPair.getPublic());
        assertThat(privateKey).isEqualTo(sourceKeyPair.getPrivate());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenFileDoesNotExist() {
        // given
        var nonExistentPath = tempDir.resolve("non-existent-file.json").toString();

        // when then
        assertThatThrownBy(() -> new JwtKeyProvider(nonExistentPath))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to load RSA key pair from Vault")
                .cause()
                .isInstanceOf(IOException.class);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenJsonIsCorrupt() throws IOException {
        // given
        var corruptFile = tempDir.resolve("corrupt.json").toFile();
        Files.writeString(corruptFile.toPath(), "{ \"rsa-private-key\": \"abc\"");

        // WHEN / THEN
        assertThatThrownBy(() -> new JwtKeyProvider(corruptFile.getAbsolutePath()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to load RSA key pair from Vault");
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenKeyIsMissingInJson() throws IOException {
        // given
        var incompleteFile = tempDir.resolve("incomplete.json").toFile();
        var jsonContent = "{ \"rsa-public-key\": \"abc\" }"; // Missing private key
        Files.writeString(incompleteFile.toPath(), jsonContent);

        // when then
        assertThatThrownBy(() -> new JwtKeyProvider(incompleteFile.getAbsolutePath()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to load RSA key pair from Vault")
                .cause()
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenKeyDataIsCorrupt() throws IOException {
        // given
        var corruptDataFile = tempDir.resolve("corrupt-data.json").toFile();
        Map<String, String> keys = Map.of(
                "rsa-private-key", "not-a-valid-key",
                "rsa-public-key", "not-a-valid-key"
        );

        new ObjectMapper().writeValue(corruptDataFile, keys);

        // when then
        assertThatThrownBy(() -> new JwtKeyProvider(corruptDataFile.getAbsolutePath()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to load RSA key pair from Vault")
                .cause()
                .isInstanceOf(IllegalArgumentException.class);
    }

    private File createTestKeyFile(Path directory, KeyPair keyPair) throws IOException {
        // Step 1: Convert keys to PEM format (with headers/footers)
        var privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";
        var publicKeyPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";

        // Step 2: Base64-encode the entire PEM string (simulating Vault's secret storage)
        var privateKeyB64 = Base64.getEncoder().encodeToString(privateKeyPem.getBytes(UTF_8));
        var publicKeyB64 = Base64.getEncoder().encodeToString(publicKeyPem.getBytes(UTF_8));

        // Step 3: Create the JSON structure
        Map<String, String> vaultSecret = Map.of(
                "rsa-private-key", privateKeyB64,
                "rsa-public-key", publicKeyB64
        );

        // Step 4: Write the JSON to a temporary file
        var keyFile = directory.resolve("jwt-keys.json").toFile();
        new ObjectMapper().writeValue(keyFile, vaultSecret);

        return keyFile;
    }
}