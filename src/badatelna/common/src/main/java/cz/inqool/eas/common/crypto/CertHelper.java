package cz.inqool.eas.common.crypto;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class CertHelper {
    /**
     * Loads jks/pfx keystore based on filename extension
     * @param resource Keystore to load from
     * @param password password, can be null
     * @return Loaded keystore
     * @throws IOException
     * @throws KeyStoreException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public static KeyStore getKeyStore(Resource resource, String password) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

        try (InputStream stream = resource.getInputStream()) {
            KeyStore keyStore;

            String filename = resource.getFilename().toLowerCase();
            if (filename.endsWith("pfx") || filename.endsWith("p12")) {
                keyStore = KeyStore.getInstance("PKCS12");
            } else {
                keyStore = KeyStore.getInstance("JKS");
            }

            char[] passwordChars = password != null ? password.toCharArray() : null;
            keyStore.load(stream, passwordChars);

            return keyStore;
        }
    }
}
