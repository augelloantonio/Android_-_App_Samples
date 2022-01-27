package com.gello94.aws_iot_connection;

import android.os.Build;
import androidx.annotation.RequiresApi;
import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public final class CertificateLoader {
    /**
     * reads a public key from a file
     * @param f file to read
     * @return the read private key
     * @throws Exception
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static PrivateKey getPrivateKeyFromPem(File f)
            throws Exception
    {

        // Read the file allocating the key --> Would prefer not to store into the phone memory
        byte[] keyBytes = Files.readAllBytes(f.toPath());

        String temp = new String(keyBytes);
        String publicKeyPEM = temp;

        // Check if file is a PCKS#1 or PCKS#8 and remove header and footer
        if(temp.contains("-----BEGIN PUBLIC KEY-----"))
        {
            publicKeyPEM = temp
                    .replace("-----BEGIN PUBLIC KEY-----\n", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .trim();
        }
        else if(temp.contains("-----BEGIN RSA PRIVATE KEY-----"))
        {
            publicKeyPEM = temp
                    .replace("-----BEGIN RSA PRIVATE KEY-----\n", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .trim();
        }

        // After header and footer removal the key is decoded into base64 and saved as
        // encoded PCKS#8 private key
        byte[] decoded = Base64.getMimeDecoder().decode(publicKeyPEM);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        // Returning a private key
        return kf.generatePrivate(spec);
    }
}