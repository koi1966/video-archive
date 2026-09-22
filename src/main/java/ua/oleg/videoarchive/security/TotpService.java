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

    public TotpService(@Value("${app.admin.issuer:VideoArchive}") String issuer) {
        this.issuer = issuer;
    }

    public GoogleAuthenticatorKey createKey() {
        return authenticator.createCredentials();
    }

    public boolean verify(String secret, int code) {
        return authenticator.authorize(secret, code);
    }

    public String otpAuthUri(String username, String secret) {
        return "otpauth://totp/" + url(issuer + ":" + username)
                + "?secret=" + url(secret)
                + "&issuer=" + url(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    private String url(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    public String qrBase64(String text) throws WriterException, IOException {
        BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 300, 300);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
