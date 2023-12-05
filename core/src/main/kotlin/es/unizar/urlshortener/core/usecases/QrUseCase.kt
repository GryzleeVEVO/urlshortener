@file:Suppress("WildcardImport")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
// core/usecases/QrCodeUseCase.kt

import es.unizar.urlshortener.core.QrCodeService


// Tengo que comprobar si con esta ID existe una URL y que se puede generar un codigo QR
// Si falla lanzar excecpiones (400 / 404) Not found se lanzaria aqui dentro
interface QrUseCase {
    fun canGenerateQrCode(id: String): Boolean
    fun createQrCode(redirectUrl: String): String
}

class QrCodeUseCaseImpl
    (private val qrCodeService: QrCodeService,
     private val shortUrlRepository: ShortUrlRepositoryService
            )
    : QrUseCase {

    override fun canGenerateQrCode(id: String): Boolean {
        val shortUrl = shortUrlRepository.findByKey(id)
            ?: throw RedirectionNotFound("Short URL with ID $id not found") // 404 Not Found
        println(shortUrl)
        shortUrl.properties.qr = true
        return true
    }

    override fun createQrCode(redirectUrl: String): String {

        return qrCodeService.generateQrCode(redirectUrl)
    }

}
