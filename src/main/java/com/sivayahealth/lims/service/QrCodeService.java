package com.sivayahealth.lims.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.sivayahealth.lims.exception.LimsException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;

@Service
public class QrCodeService {

    private static final int QR_SIZE = 200;

    public byte[] generateQrPng(String content) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw LimsException.notFound("Failed to generate QR code: " + e.getMessage());
        }
    }

    public String generateQrBase64(String content) {
        return Base64.getEncoder().encodeToString(generateQrPng(content));
    }

    /**
     * Builds the QR payload for a chemical bottle:
     * format: CHEM|<4-letter-prefix>|T<tenantId>|B<branchId>|<regNo>|EXP:<expiryDate>
     */
    public String buildChemicalQrPayload(String chemicalName, Long tenantId, Long branchId,
                                         String regNo, String expiryDate) {
        String prefix = chemicalName != null && chemicalName.length() >= 4
                ? chemicalName.substring(0, 4).toUpperCase()
                : (chemicalName != null ? chemicalName.toUpperCase() : "CHEM");
        return String.format("CHEM|%s|T%d|B%d|%s|EXP:%s", prefix, tenantId, branchId, regNo, expiryDate);
    }
}
