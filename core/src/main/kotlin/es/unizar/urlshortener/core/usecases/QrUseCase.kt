@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.QrCodeNotFound// core/usecases/QrCodeUseCase.kt

import es.unizar.urlshortener.core.QrCodeService
import es.unizar.urlshortener.core.RedirectionNotFound
import es.unizar.urlshortener.core.ShortUrlRepositoryService


interface QrUseCase {
    /**
     * Checks if a QR code can be generated for the specified short URL identifier.
     *
     * @param id The short URL identifier for which the QR code generation is queried.
     * @return `true` if a QR code can be generated; otherwise, `false`.
     */
    fun canGenerateQrCode(id: String): Boolean
    /**
     * Creates a QR code for the provided redirection URL.
     *
     * @param redirectUrl The URL to which the QR code will redirect.
     * @return The generated QR code as a ByteArray.
     */
    fun createQrCode(redirectUrl: String): ByteArray
}

class QrCodeUseCaseImpl
    (private val qrCodeService: QrCodeService,
     private val shortUrlRepository: ShortUrlRepositoryService
            )
    : QrUseCase {

    override fun canGenerateQrCode(id: String): Boolean {
        val shortUrl = shortUrlRepository.findByKey(id)
            ?: throw RedirectionNotFound("Short URL with ID $id not found") // 404 Not Found


        //println(shortUrl)

        if (shortUrl.properties.qr == null || !shortUrl.properties.qr!!) {
            throw QrCodeNotFound(id)
        } else return shortUrl.properties.qr!!
    }

    override fun createQrCode(redirectUrl: String): ByteArray {

        return qrCodeService.generateQrCode(redirectUrl)
    }

}
