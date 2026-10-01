package ua.oleg.videoarchive.security;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
public class TotpService {
    private final GoogleAuthenticator authenticator = new GoogleAuthenticator();
    private final String issuer;

    /**
     * Створює сервіс TOTP та отримує назву видавця (`issuer`), яка використовується під час формування даних для Google Authenticator.
     * @param issuer назва видавця TOTP
     */
    public TotpService(@Value("${app.admin.issuer:VideoArchive}") String issuer) {
        this.issuer = issuer;
    }

    /**
     * Генерує новий секретний ключ TOTP через бібліотеку Google Authenticator. Секрет згодом зберігається для користувача у MongoDB.
     * @return результат роботи методу (GoogleAuthenticatorKey)
     */
    public GoogleAuthenticatorKey createKey() {
        return authenticator.createCredentials();
    }

    /**
     * Перевіряє введений 6-значний TOTP-код щодо збереженого секретного ключа та поточного часу сервера.
     * @param secret секрет TOTP
     * @param code введений 6-значный TOTP-код
     * @return результат роботи методу (boolean)
     */
    public boolean verify(String secret, int code) {
        return authenticator.authorize(secret, code);
    }

    /**
     * Формує стандартний `otpauth://` URI для реєстрації користувача у Google Authenticator.
     * @param username логін користувача
     * @param secret секрет TOTP
     * @return результат роботи методу (String)
     */
    public String otpAuthUri(String username, String secret) {
        return "otpauth://totp/" + url(issuer + ":" + username)
                + "?secret=" + url(secret)
                + "&issuer=" + url(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    /**
     * URL-кодує значення, щоб воно безпечно ввійшло у otpauth URI.
     * @param value значення, яке необходимо обработать
     * @return результат роботи методу (String)
     */
    private String url(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Створює QR-код з otpauth URI, кодує зображення у Base64 та повертає рядок, який можна безпосередньо використовувати у HTML.
     * @param text текст, із якого формируется QR-код
     * @return результат роботи методу (String)
     */
    public String qrBase64(String text) throws WriterException, IOException {
        BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 300, 300);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
