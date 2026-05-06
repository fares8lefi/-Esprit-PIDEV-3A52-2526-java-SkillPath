package Utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to generate QR Code images using the ZXing library.
 * Returns a JavaFX Image directly for use in ImageView nodes.
 */
public class QRCodeGenerator {

    /**
     * Generates a QR code as a JavaFX Image.
     *
     * @param content The text/data to encode in the QR code.
     * @param width   Desired width in pixels.
     * @param height  Desired height in pixels.
     * @return A JavaFX {@link Image} of the QR code, or null on error.
     */
    public static Image generateQRCode(String content, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            WritableImage fxImage = new WritableImage(width, height);
            PixelWriter pw = fxImage.getPixelWriter();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Black modules on white background
                    pw.setColor(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return fxImage;

        } catch (WriterException e) {
            System.err.println("[QRCodeGenerator] Failed to generate QR code: " + e.getMessage());
            return null;
        }
    }

    /**
     * Builds the encoded ticket data string for an event registration.
     *
     * @param eventId   Event ID
     * @param userId    User UUID string
     * @param eventTitle Event title
     * @param eventDate  Event date string
     * @param seatNumber Seat number (null if no seat)
     * @return Encoded string for the QR code
     */
    public static String buildTicketData(int eventId, String userId, String eventTitle,
                                         String eventDate, Integer seatNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append("SKILLPATH-TICKET\n");
        sb.append("Event: ").append(eventTitle).append("\n");
        sb.append("Date: ").append(eventDate).append("\n");
        sb.append("EventID: ").append(eventId).append("\n");
        sb.append("UserID: ").append(userId).append("\n");
        if (seatNumber != null) {
            sb.append("Seat: ").append(seatNumber).append("\n");
        }
        sb.append("Issuer: SkillPath Platform");
        return sb.toString();
    }
}
