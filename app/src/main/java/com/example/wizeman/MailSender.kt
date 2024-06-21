package com.example.wizeman

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import com.google.ai.client.generativeai.GenerativeModel

class MailSender(private val username: String, private val appPassword: String) {

    private suspend fun getGeminiResponse(prompt: String): String {
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = "AIzaSyBnYcO9geA8JOgXOGyN9kkp56iK8OeJy2A"
        )

        return try {
            val response = generativeModel.generateContent(prompt)
            response.text.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "An error occurred: ${e.message}"
        }
    }

    private val TAG = "MailSender"
    private lateinit var firebaseServices: FirebaseServices
    suspend fun sendMail(subject: String, body: String, recipients: List<String>, imageBytes: ByteArray, attachmentPath: String, latitude: Double, longitude: Double) {
        withContext(Dispatchers.IO) {
            val props = Properties().apply {
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.socketFactory.port", "465")
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.auth", "true")
                put("mail.smtp.port", "465")
            }

            firebaseServices = FirebaseServices(this);

            val session = Session.getDefaultInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(username, appPassword)
                }
            })

            try {
                val weatherService = WeatherService("d8dae9545723a272e9fce64b0227f85f12c0099d2913e40f50baa84768974434")
                val weatherResponse = weatherService.getWeather(latitude, longitude)

                val weatherInfo = weatherResponse?.forecast?.firstOrNull()?.let {
                    "Température min: ${it.tmin}°C, Température max: ${it.tmax}°C"
                } ?: "Météo non disponible"

                //val fullBody = "$body\n\nPosition: L = $longitude et l = $latitude\n\nMétéo actuelle: $weatherInfo"

                val response = getGeminiResponse("bonjour, presente toi et donne moi la localisation, la ville, le pays et le quartier dont la latitude est $latitude et la longitude est $longitude")

                val fullBody = "$body\n\nPosition: L = $longitude et l = $latitude\n\nMétéo actuelle: $weatherInfo\n\n$response"

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(username))
                    val addressArray = recipients.map { InternetAddress(it) }.toTypedArray()
                    setRecipients(Message.RecipientType.TO, addressArray)
                    setSubject(subject)
                    setText(fullBody)
                }

                val multipart = MimeMultipart().apply {
                    val messageBodyPart = MimeBodyPart()
                    messageBodyPart.setText(fullBody)
                    addBodyPart(messageBodyPart)

                    val attachBodyPart = MimeBodyPart()
                    attachBodyPart.dataHandler = DataHandler(FileDataSource(attachmentPath))
                    attachBodyPart.fileName = attachmentPath.split("/").last()
                    addBodyPart(attachBodyPart)
                }

                message.setContent(multipart)

                Transport.send(message)
                Log.d(TAG, "Email sent successfully")
                firebaseServices.saveImageToFirebase(imageBytes, latitude, longitude, weatherInfo);
            } catch (e: MessagingException) {
                Log.e(TAG, "Error in sending email", e)
            }
        }
    }
}
