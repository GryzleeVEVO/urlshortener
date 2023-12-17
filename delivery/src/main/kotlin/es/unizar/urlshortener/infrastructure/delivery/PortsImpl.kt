package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.QrCodeService
import es.unizar.urlshortener.core.ValidatorService
import org.apache.commons.validator.routines.UrlValidator
import java.nio.charset.StandardCharsets
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.client.j2se.MatrixToImageWriter
import java.io.ByteArrayOutputStream

import es.unizar.urlshortener.core.CsvService
import com.opencsv.CSVReader
import java.io.File
import java.io.FileReader

/**
 * Implementation of the port [ValidatorService].
 */
class ValidatorServiceImpl : ValidatorService {
    override fun isValid(url: String) = urlValidator.isValid(url)

    companion object {
        val urlValidator = UrlValidator(arrayOf("http", "https"))
    }
}

/**
 * Implementation of the port [HashService].
 */
class HashServiceImpl : HashService {

    //VERSION BUENA
    override fun hasUrl(url: String, customText: String): String { //, prueba: String
        // val baseHash = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
        // val regex = Regex("[a-zA-Z0-9]+")
        if (customText == ""){ //|| !customText.matches(regex)
            val modifiedHash = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
            return modifiedHash
        }else{
            val modifiedHash = customText
            return modifiedHash
        }
    }
}

class CsvServiceImpl : CsvService {
    override fun csvHasUrl(csvFile: List<String>, customWords: List<String>): List<String> {
        // Lista dinámica para almacenar el resultado
        val processedUrls = mutableListOf<String>()

        // Iterar sobre cada URL en la lista
        for (i in csvFile.indices) {
            // Instancia de hasServiceImp
            val hashServiceInstance = HashServiceImpl()

            // Procesar la URL con la función hasUrl
            // (por ahora no se tiene en cuenta el custom, habría que tener una lista de string)
            val result = hashServiceInstance.hasUrl(csvFile[i], customWords[i]) //customText

            // Agregar el resultado a la lista de URLs procesadas
            processedUrls.add(result)
        }

        // Devolver la lista de URLs procesadas
        return processedUrls
    }
}

/**
 * Implementation of the port [QrCodeService].
 */
class QrCodeServiceImpl : QrCodeService {

    companion object {
        const val QR_CODE_WIDTH = 300
        const val QR_CODE_HEIGHT = 300
    }
    override fun generateQrCode(url: String): ByteArray {
        val writer = QRCodeWriter()
        val bitMatrix: BitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT)

        val stream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", stream)

        val qrCodeBytes = stream.toByteArray()

        return qrCodeBytes
    }
}
