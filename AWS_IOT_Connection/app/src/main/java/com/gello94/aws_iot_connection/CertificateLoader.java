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
     * @return the read public key
     * @throws Exception
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static PrivateKey getPublicKeyFromPem(File f)
            throws Exception
    {
        byte[] keyBytes = Files.readAllBytes(f.toPath());

        String temp = new String(keyBytes);
        String publicKeyPEM = temp;

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

        byte[] decoded = Base64.getMimeDecoder().decode(publicKeyPEM);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}