@file:Suppress("WildcardImport")
package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.usecases.*
import jakarta.servlet.http.HttpServletRequest
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Unit>

    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>



    fun statistics(id:String, request: HttpServletRequest): ResponseEntity<ClickStatsDataOut>

    /**
     * Generates a QR code for the provided short URL identifier [id].
     *
     * Note: Utilizes the [QrUseCase] for QR code creation.
     */
    fun getQrCode(id: String, request: HttpServletRequest): ResponseEntity<ByteArray>

}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val sponsor: String? = null,
    val customText: String,
    val qr: Boolean = false
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)

/**
 * Data returned after getting statistics for logged clicks for a short URL.
 */
data class ClickStatsDataOut(
        val url: URI, val clicks: List<ClickProperties> = emptyList()
)

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlShortenerControllerImpl(
    val redirectUseCase: RedirectUseCase,
    val logClickUseCase: LogClickUseCase,
    val createShortUrlUseCase: CreateShortUrlUseCase,
    val qrCodeUseCase: QrUseCase,
    val parseHeaderUseCase: ParseHeaderUseCase,
    val getGeolocationUseCase: GetGeolocationUseCase
) : UrlShortenerController {

    @GetMapping("/{id:(?!api|index).*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Unit> =
        redirectUseCase.redirectTo(id).let {
            val ipData = ClickProperties(ip = request.remoteAddr)
            val userAgentData = parseHeaderUseCase.parseHeader(request.getHeader("User-Agent"), ipData)
            val data  = getGeolocationUseCase.getGeolocation(request.remoteAddr, userAgentData)

            logClickUseCase.logClick(id, data)
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Unit>(h, HttpStatus.valueOf(it.mode))
        }

    @PostMapping("/api/link", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor,
                qr = data.qr
            ),
            customText = data.customText
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            val response = ShortUrlDataOut(
                url = url,
                properties = buildMap {
                    set("safe", it.properties.safe)
                    if (data.qr){
                        set("qr", "$url/qr")
                    }
                }
            )
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }

    @GetMapping("/{id:(?!api|index).*}/qr", produces = [MediaType.IMAGE_PNG_VALUE])
    override fun getQrCode(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<ByteArray> {

        println("GET QR Code called for id: $id")
        val qrCode = qrCodeUseCase.canGenerateQrCode(id)
        println(qrCode)

        if (qrCode) {
            val redirectUrl = linkTo<UrlShortenerControllerImpl> { redirectTo(id, request) }.toUri().toString()
            println(redirectUrl)
            val qrCodeBytes = qrCodeUseCase.createQrCode(redirectUrl)

            val headers = HttpHeaders()
            headers.contentType = MediaType.IMAGE_PNG

            return ResponseEntity(qrCodeBytes, headers, HttpStatus.OK)
        }

        return ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @GetMapping("/api/link/{id:(?!api|index).*}")
    override fun statistics(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<ClickStatsDataOut> {
        val url = linkTo<UrlShortenerControllerImpl> { redirectTo(id, request) }.toUri()

        logClickUseCase.getClicksByShortUrlHash(id).let {
            val response = ClickStatsDataOut(
                    url = url, clicks = it
            )
            return ResponseEntity<ClickStatsDataOut>(response, HttpStatus.OK)
        }
    }
}
