package com.gello94.aws_iot_connection

import android.os.Build
import android.util.Log
import android.widget.TextView
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread
import com.gello94.aws_iot_connection.CertificateLoader.getPublicKeyFromPem
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.io.*
import java.lang.Exception
import java.net.ConnectException
import java.security.*
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.RSAPublicKeySpec

object oAuthHttpConnection {

    fun postData(jsonData: String, url:String, textResult: TextView) {

        try {
            val caInput: InputStream = BufferedInputStream(FileInputStream("#Path of your certificate somename_root.pem")) // Should be the AmazonRoot1.pem

            val key: PrivateKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getPublicKeyFromPem(File("#Path of your certificate somename_private.pem.key"))
            } else {
                TODO("VERSION.SDK_INT < O")
            }

            val publicKeySpec = getPublicKeySpec(key)
            val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
            val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)

            val keyPair = KeyPair(publicKey, key)

            val cf = CertificateFactory.getInstance("X.509")
            val caInput2: InputStream =
                BufferedInputStream(FileInputStream("#Path of your  somename_certificate.pem.crt"))
            val ca: Certificate

            try {
                ca = cf.generateCertificate(caInput2)
                println("ca=" + (ca as X509Certificate).subjectDN)

                // Client certificate, used to authenticate the client
                val clientCertificate = HeldCertificate(keyPair, (ca as X509Certificate))

                caInput.close()

                // Create handshake certificate (which root certificate/hostname to trust)
                val clientCertificatesBuilder = HandshakeCertificates.Builder()
                    .addPlatformTrustedCertificates() // trust all certificate which are trusted on the platform
                    .heldCertificate(clientCertificate) // attach the client certificate

                clientCertificatesBuilder.addInsecureHost(Build.HOST)
                val clientCertificates = clientCertificatesBuilder.build()

                val okHttpClient: OkHttpClient = OkHttpClient.Builder()
                    .sslSocketFactory(
                        clientCertificates.sslSocketFactory(),
                        clientCertificates.trustManager
                    )
                    .build()

                val mediaType = "text/plain".toMediaTypeOrNull()
                val body: RequestBody = RequestBody.create(
                    mediaType,jsonData
                )

                val request = Request.Builder()
                    .url(url)
                    .method("POST", body)
                    .addHeader("Content-Type", "text/plain")
                    .build();

                try {
                    val httpResponse: Response = okHttpClient.newCall(request).execute()

                    Log.i("httpResponse: ", httpResponse.code.toString())

                    if (httpResponse.code == 200){

                        runOnUiThread(Runnable {
                            textResult.text = "Message Send: " + jsonData
                        })
                    } else {
                        runOnUiThread(Runnable {
                            textResult.text = "Message Not Send"
                        })
                    }


                } catch (e:Exception){
                    println(e)
                    runOnUiThread(Runnable {
                        textResult.text = "Message Not Send"
                    })
                } catch (e: ConnectException){
                    println(e)
                    runOnUiThread(Runnable {
                        textResult.text = "Message Not Send"
                    })
                }
            } finally {
                caInput.close()
            }

        } catch (e: Exception) {
            Log.d("Er/request", e.toString())
            runOnUiThread(Runnable {
                textResult.text = "Message Not Send"
            })
        }
    }

    fun getPublicKeySpec(priv: PrivateKey): RSAPublicKeySpec? {
        val rsaCrtKey: RSAPrivateCrtKey = priv as RSAPrivateCrtKey // May throw a ClassCastException
        return RSAPublicKeySpec(rsaCrtKey.getModulus(), rsaCrtKey.getPublicExponent())
    }
}