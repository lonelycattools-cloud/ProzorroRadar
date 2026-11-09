package ua.gov.prozorro.radar.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ua.gov.prozorro.radar.data.AppPreferences
import ua.gov.prozorro.radar.model.Tender
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class EmailSender(private val context: Context) {

    private val preferences = AppPreferences(context)

    suspend fun testSmtpConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val host = preferences.smtpHost.trim()
            val port = preferences.smtpPort
            val user = preferences.smtpUser.trim()
            val pass = preferences.smtpPass.replace(" ", "").trim()

            if (host.isBlank() || user.isBlank() || pass.isBlank()) {
                return@withContext Result.failure(Exception("Заповніть хост, логін та пароль SMTP"))
            }

            executeSmtpTransaction(
                host = host,
                port = port,
                user = user,
                pass = pass,
                from = user,
                to = listOf(user),
                replyTo = user,
                subject = "Prozorro Radar: Тестове повідомлення",
                bodyHtml = "<h3>Тест SMTP зв\'язку успішний!</h3><p>Prozorro Радар готовий до розсилки сповіщень через скриньку $user.</p>"
            )
            Result.success("З'єднання успішно встановлено! Тестовий лист надіслано на $user.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendTendersDigest(tenders: List<Tender>): Result<Int> = withContext(Dispatchers.IO) {
        val recipientsStr = preferences.recipientEmails
        if (recipientsStr.isBlank() || tenders.isEmpty()) {
            return@withContext Result.success(0)
        }

        val recipients = recipientsStr.split(",").map { it.trim() }.filter { it.contains("@") }
        if (recipients.isEmpty()) {
            return@withContext Result.success(0)
        }

        val host = preferences.smtpHost.trim()
        val port = preferences.smtpPort
        val user = preferences.smtpUser.trim()
        val pass = preferences.smtpPass.replace(" ", "").trim()

        if (host.isBlank() || user.isBlank() || pass.isBlank()) {
            return@withContext Result.failure(Exception("SMTP не налаштовано в Налаштуваннях"))
        }

        val htmlBody = buildHtmlDigest(tenders)
        val subject = "Prozorro Radar: Знайдено ${tenders.size} нових тендерів"

        try {
            executeSmtpTransaction(
                host = host,
                port = port,
                user = user,
                pass = pass,
                from = user,
                to = recipients,
                replyTo = user,
                subject = subject,
                bodyHtml = htmlBody
            )
            Result.success(tenders.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFeedbackDirect(
        senderEmail: String,
        type: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val host = preferences.smtpHost.trim()
        val port = preferences.smtpPort
        val user = preferences.smtpUser.trim()
        val pass = preferences.smtpPass.replace(" ", "").trim()

        if (host.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
            try {
                val subject = "[Prozorro Radar] $type від $senderEmail"
                val bodyHtml = """
                    <h3>Нове повідомлення від користувача Prozorro Radar</h3>
                    <p><b>Від кого:</b> <a href="mailto:$senderEmail">$senderEmail</a></p>
                    <p><b>Категорія:</b> $type</p>
                    <hr/>
                    <p style="white-space: pre-wrap;">${message.replace("<", "&lt;").replace(">", "&gt;")}</p>
                """.trimIndent()

                executeSmtpTransaction(
                    host = host,
                    port = port,
                    user = user,
                    pass = pass,
                    from = user,
                    to = listOf(DEV_EMAIL),
                    replyTo = senderEmail,
                    subject = subject,
                    bodyHtml = bodyHtml
                )
                return@withContext Result.success("Повідомлення надіслано!")
            } catch (e: Exception) {
                // Fallback to intent
            }
        }
        Result.failure(Exception("SMTP не налаштовано або помилка з'єднання"))
    }

    fun openEmailClientForFeedback(
        context: Context,
        senderEmail: String,
        type: String,
        message: String
    ) {
        val subject = "[Prozorro Radar] $type від $senderEmail"
        val body = """
            Зворотний зв'язок від користувача:
            Email: $senderEmail
            Тема: $type
            
            Повідомлення:
            $message
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$DEV_EMAIL")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(DEV_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Надіслати відгук").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildHtmlDigest(tenders: List<Tender>): String {
        return buildString {
            append("""
                <html>
                <body style="font-family: Arial, sans-serif; background: #f8fafc; padding: 20px; color: #1e293b;">
                <div style="max-width: 650px; margin: auto; background: #ffffff; border-radius: 12px; padding: 24px; border: 1px solid #e2e8f0;">
                    <div style="display: flex; align-items: center; border-bottom: 2px solid #00C853; padding-bottom: 12px; margin-bottom: 20px;">
                        <h2 style="color: #00A86B; margin: 0;">Prozorro Радар</h2>
                        <span style="margin-left: auto; color: #64748b; font-size: 13px;">${tenders.size} нових торгів</span>
                    </div>
            """.trimIndent())

            for (t in tenders) {
                append("""
                    <div style="border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin-bottom: 16px; background: #fdfdfd;">
                        <h3 style="margin-top: 0; color: #0f172a; font-size: 16px;">
                            <a href="${t.tenderUrl}" style="color: #0284c7; text-decoration: none;">${t.title}</a>
                        </h3>
                        <p style="margin: 6px 0; font-size: 14px;">
                            <b>Бюджет:</b> <span style="color: #059669; font-weight: bold; font-size: 15px;">${t.formattedBudget}</span>
                        </p>
                        <p style="margin: 4px 0; font-size: 13px; color: #475569;">
                            <b>Замовник:</b> ${t.procuringEntityName} ${if (t.procuringEntityEdrpou.isNotEmpty()) "(ЄДРПОУ: ${t.procuringEntityEdrpou})" else ""}
                        </p>
                        <p style="margin: 4px 0; font-size: 13px; color: #475569;">
                            <b>Класифікатор CPV:</b> ${t.cpvCodes}
                        </p>
                        <p style="margin: 4px 0; font-size: 13px; color: #dc2626;">
                            <b>Дедлайн:</b> ${t.deadline}
                        </p>
                        <div style="margin-top: 10px;">
                            <a href="${t.tenderUrl}" style="display: inline-block; background: #00C853; color: white; padding: 8px 16px; border-radius: 6px; text-decoration: none; font-size: 13px; font-weight: bold;">
                                Переглянути на Prozorro →
                            </a>
                        </div>
                    </div>
                """.trimIndent())
            }

            append("""
                    <div style="font-size: 12px; color: #94a3b8; text-align: center; margin-top: 24px;">
                        Лист згенеровано автоматично додатком Prozorro Радар.
                    </div>
                </div>
                </body>
                </html>
            """.trimIndent())
        }
    }

    private fun executeSmtpTransaction(
        host: String,
        port: Int,
        user: String,
        pass: String,
        from: String,
        to: List<String>,
        replyTo: String,
        subject: String,
        bodyHtml: String
    ) {
        val cleanHost = host.trim()
        val cleanUser = user.trim()
        val cleanPass = pass.replace(" ", "").trim()
        val cleanFrom = from.trim().ifEmpty { cleanUser }

        val socket = Socket(cleanHost, port)
        socket.soTimeout = 20000

        var reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        var writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)

        fun readLine(): String = reader.readLine() ?: ""
        fun send(cmd: String) {
            writer.print("$cmd\r\n")
            writer.flush()
        }

        readLine() // Greeting
        send("EHLO localhost")
        var ehloResponse = ""
        while (true) {
            val line = readLine()
            ehloResponse += line + "\n"
            if (line.length >= 4 && line[3] == ' ') break
        }

        // STARTTLS if port 587
        if (port == 587 || ehloResponse.contains("STARTTLS", ignoreCase = true)) {
            send("STARTTLS")
            readLine() // 220
            val sslSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                .createSocket(socket, cleanHost, port, true) as SSLSocket
            sslSocket.startHandshake()
            reader = BufferedReader(InputStreamReader(sslSocket.getInputStream()))
            writer = PrintWriter(OutputStreamWriter(sslSocket.getOutputStream()), true)

            send("EHLO localhost")
            while (true) {
                val line = readLine()
                if (line.length >= 4 && line[3] == ' ') break
            }
        }

        // AUTH LOGIN
        send("AUTH LOGIN")
        readLine() // 334 Username:
        send(Base64.encodeToString(cleanUser.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
        readLine() // 334 Password:
        send(Base64.encodeToString(cleanPass.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
        val authResult = readLine()
        if (!authResult.startsWith("235")) {
            throw Exception("SMTP Auth Failed: $authResult")
        }

        // MAIL FROM
        send("MAIL FROM:<$cleanFrom>")
        readLine()

        // RCPT TO
        for (recipient in to) {
            val cleanRecipient = recipient.trim()
            if (cleanRecipient.isNotEmpty()) {
                send("RCPT TO:<$cleanRecipient>")
                readLine()
            }
        }

        // DATA
        send("DATA")
        readLine() // 354

        // Headers
        send("From: Prozorro Radar <$cleanFrom>")
        send("To: ${to.joinToString(", ")}")
        send("Reply-To: ${replyTo.trim().ifEmpty { cleanUser }}")
        send("Subject: =?UTF-8?B?${Base64.encodeToString(subject.toByteArray(), Base64.NO_WRAP)}?=")
        send("MIME-Version: 1.0")
        send("Content-Type: text/html; charset=UTF-8")
        send("Content-Transfer-Encoding: base64")
        send("")

        // Encoded HTML body
        val base64Body = Base64.encodeToString(bodyHtml.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        send(base64Body)
        send(".")
        val endResult = readLine()
        if (!endResult.startsWith("250")) {
            throw Exception("SMTP Send Failed: $endResult")
        }

        send("QUIT")
        try { socket.close() } catch (ignored: Exception) {}
    }

    companion object {
        const val DEV_EMAIL = "lonelycattools@gmail.com"
    }
}
