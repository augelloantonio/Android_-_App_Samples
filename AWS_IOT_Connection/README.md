# AWS IOT CONNECTION APP.

![Main Page](https://github.com/gello94/Android_-_App_Samples/blob/main/images/aws_iot_screen.png)

## Dependencies

Dependencies needed for this project to add to gradle module are (if more recent versions use them):

- implementation 'com.amazonaws:aws-android-sdk-iot:2.37.1'
- implementation 'com.amazonaws:aws-android-sdk-mobile-client:2.37.1'
- implementation("com.squareup.okhttp3:okhttp:4.9.0")
- implementation 'com.squareup.okhttp3:okhttp-tls:4.9.3'

## Certificates

Security Certificate are the main core AWS security. Once you have created your object in AWS IOT
Console you will have 3 main certificate to use:

- "somename.pem.crt"
- "somename.pem.key"
- "somenameRoot.pem"

To connect you don't really need the "somenameRoot.pem" certificate.

The "somename.pem.key" is an PKCS #1 format and to work with it in Android we need to use a PKCS #8
format.

#### Note: for this project I used the certificates file in the local storage, but for security reasons would prefer more secure ways.

With the following Java Class you can convert the "somename.pem.key" in your Android app:

```
  public final class CertificateLoader {
    /**
     * reads a public key from a file
     * @param f file to read
     * @return the read private key
     * @throws Exception
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static PrivateKey getPublicKeyFromPem(File f)
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
```

After we have our certificate converted in the AWSConnection object we can load the other
certificate "somename.pem.crt". We need to associate this cert to the loaded key.

In first we need to load the private key calling the above class:

```
    val key: PrivateKey = getPrivateKeyFromPem(File("#Path of your certificate somename.pem.crt")
    
    
```

Then we need to generate a PublicKey using "RSA" algorithm:

```

    val publicKeySpec = getPublicKeySpec(key)
    val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
    val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)
    
```

Then we need to use KeyPair:

```

    val keyPair = KeyPair(publicKey, key)
  
```

Now we need to load the certificate "somename.pem.crt"

```

val cf = CertificateFactory.getInstance("X.509")
val caInput: InputStream =BufferedInputStream(FileInputStream("#Path of your  somename.pem.crt"))
val ca: Certificate

ca = cf.generateCertificate(caInput)

// Client certificate, used to authenticate the client
val clientCertificate = HeldCertificate(keyPair, (ca as X509Certificate))

```

All the certificate are now ready to be used.

## Create the request with OkHttp3

To use the certificates in OkHttp3 we need to build a sslSocketFactory.
To do so we first create an handshake certificate, which root certificate/hostname to trust.

```

val clientCertificatesBuilder = HandshakeCertificates.Builder()
.addPlatformTrustedCertificates() // trust all certificate which are trusted on the platform
.heldCertificate(clientCertificate) // attach the client certificate

```

Then we have to build the clientCertificates.
At this point we can skip the use of the TrustManager certificate, our "somenameRoot.pem", trusting all certificates or we can use it.

Trust all certificates without a given Trusted certificate:

```

clientCertificatesBuilder.addInsecureHost(Build.HOST)
val clientCertificates = clientCertificatesBuilder.build()

```

Use a Trusted certificate (recommended):

```
val caInput2: InputStream =
                BufferedInputStream(FileInputStream("#Path of your  somenameRoot.pem"))

// Client certificate, used to authenticate the client
val clientTrustedCertificate = HeldCertificate(keyPair, (ca2 as X509Certificate))

clientCertificatesBuilder.addTrustedCertificate(clientTrustedCertificate)
val clientCertificates = clientCertificatesBuilder.build()

```

After we have our sslSocketFactory and our clientCertificates we can build the okHttp3 instance:

```

val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .sslSocketFactory(
            clientCertificates.sslSocketFactory(),
            clientCertificates.trustManager
        )
    .build()
    
```

And now make a request:

```

val mediaType = "text/plain".toMediaTypeOrNull()
    val body: RequestBody = RequestBody.create(
        mediaType,jsonData
    )

val request = Request.Builder()
    .url(url)
    .method("POST", body)
    .addHeader("Content-Type", "text/plain")
    .build();

```