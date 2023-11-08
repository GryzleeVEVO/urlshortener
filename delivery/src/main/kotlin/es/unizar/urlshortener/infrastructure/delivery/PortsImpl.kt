package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorService
import org.apache.commons.validator.routines.UrlValidator
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
    // VERSION DEFAULT
    // override fun hasUrl(url: String) = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
    
    // MI VERSION AÑADIENDO A LA URL EL TEXTO "EJEMPLO"
    // override fun hasUrl(url: String): String {
    //     val baseHash = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
    //     val modifiedHash = baseHash + "ejemplo"
    //     return modifiedHash
    // }

    // MI VERSION SOLO EJEMPLO EN LA URL
    override fun hasUrl(url: String): String {
        //val baseHash = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
        val modifiedHash = "ejemplo"
        return modifiedHash
    }
}
