@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

//he añadido al build.gradle de estas carpetas la dependencia
import kotlinx.coroutines.*

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, data: ShortUrlProperties, customText: String): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService
) : CreateShortUrlUseCase {
    // con runBlocking conseguimos una  (ejecucion asincrona)
    override fun create(url: String, data: ShortUrlProperties, customText: String): ShortUrl = runBlocking {
        val id: String = hashService.hasUrl(url,customText)

        // con "?" realizamos la accion solo si findByKey no devuelve null
        return@runBlocking shortUrlRepository.findByKey(id)?.let {
            // la ShortUrl ya existe

            // comprobamos safe (it se refiere al valor obtenido de findByKey)
            //it.properties.safe?.let { safe ->
            //    if(!safe) {
            //        // la ShortURL no es safe
            //        throw UnsafeUrlException(url) //cambiar por otra nueva de Unsafe
            //    }
            //}
            if (customText == "") {
                return@let it
            }else{
                throw UsedCustomWordException(customText)
            }
            // return@let it

        } ?: run {
            if (validatorService.isValid(url)) {
                // val id: String = hashService.hasUrl(url,customText)
                val su = ShortUrl(
                    hash = id,
                    redirection = Redirection(target = url),
                    properties = ShortUrlProperties(
                        safe = data.safe,
                        ip = data.ip,
                        sponsor = data.sponsor,
                        qr = data.qr
                    )
                )
                val shortUrlSave = shortUrlRepository.save(su)
                return@run shortUrlSave
            } else {
                throw InvalidUrlException(url)
            }
        }
    }
}
