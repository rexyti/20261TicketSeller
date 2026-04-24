package com.ticketseller.infrastructure.adapter.out.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.ticketseller.domain.repository.CodigoQrPort;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class ZxingCodigoQrAdapter implements CodigoQrPort {

    @Override
    public String generarCodigo(String contenido) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            BitMatrix matrix = new MultiFormatWriter().encode(contenido, BarcodeFormat.QR_CODE, 256, 256, hints);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible generar el QR", ex);
        }
    }
}

