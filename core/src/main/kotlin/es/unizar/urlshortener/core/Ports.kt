package es.unizar.urlshortener.core

import java.io.File

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun save(cl: Click): Click
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByKey(id: String): ShortUrl?
    fun save(su: ShortUrl): ShortUrl
}

/**
 * [ValidatorService] is the port to the service that validates if an url can be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface ValidatorService {
    fun isValid(url: String): Boolean
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
 //, prueba: String
interface HashService {
    fun hasUrl(url: String, customText: String): String  
}


/**
 * [CsvService] is the port to the service that creates a list of hash from a Csv with URLs file.
 *
 */
interface CsvService {
    fun csvHasUrl(csvFile: List<String>, customWords: List<String>): List<String>  
}

/**
 * [QrCodeService] is the port of the service that creates a QR Code from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface QrCodeService {
    fun generateQrCode(url: String): ByteArray
}
