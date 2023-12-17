@file:Suppress("WildcardImport","MatchingDeclarationName")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 * Log that somebody has requested the redirection identified by a key.
 *
 * **Note**: This is an example of functionality.
 */
    interface LogClickUseCase {
    fun logClick(key: String, data: ClickProperties)


    fun getClicksByShortUrlHash(hash: String): List<ClickProperties>
}

/**
 * Implementation of [LogClickUseCase].
 */
class LogClickUseCaseImpl(
    private val clickRepository: ClickRepositoryService,
    private val shortUrlRepository: ShortUrlRepositoryService
) : LogClickUseCase {
    override fun logClick(key: String, data: ClickProperties) {
        val cl = Click(
            hash = key,
            properties = data
        )
        clickRepository.save(cl)
    }

    override fun getClicksByShortUrlHash(hash: String) : List<ClickProperties> {
        if (shortUrlRepository.findByKey(hash) != null) {
            return clickRepository.findByShortUrlHash(hash).map { it.properties }
        } else {
            throw RedirectionNotFound(hash)
        }
    }

}
