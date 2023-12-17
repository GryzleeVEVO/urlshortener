@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.DatabaseReader.Builder
import com.maxmind.geoip2.exception.AddressNotFoundException
import es.unizar.urlshortener.core.GeolocationService
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorService
import org.apache.commons.validator.routines.UrlValidator
import org.springframework.util.ResourceUtils.getFile
import java.net.InetAddress
import java.nio.charset.StandardCharsets

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
    override fun hasUrl(url: String) = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
}

class GeolocationServiceImpl(
    private val databaseReader: DatabaseReader =
        Builder(getFile("classpath:maxmind/GeoLite2-Country.mmdb")).build()
) : GeolocationService {
    @Suppress("SwallowedException")
    override fun getCountry(ip: String): String? {
        return try {
            databaseReader.country(InetAddress.getByName(ip)).country.name
        } catch ( e: AddressNotFoundException) {
            null
        }
    }
}
