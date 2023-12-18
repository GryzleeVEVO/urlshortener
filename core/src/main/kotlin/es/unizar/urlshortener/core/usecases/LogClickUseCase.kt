@file:Suppress("WildcardImport","MatchingDeclarationName")
package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 * [LogClickUseCase] offers functionality for logging data from a redirection request. The redirect data is stored in
 * a [ClickProperties] object. The functionality offers methods for adding a new [click][ClickProperties] for a
 * redirection with a given key, and a method for obtaining a [list of clicks][List] given a key of a valid redirection
 * endpoint
 */
    interface LogClickUseCase {

    /**
     * Given a key for a redirection endpoint and a [ClickProperties] object, it stores the click data in a database.
     *
     * @param key Key of the redirection endpoint
     * @param data Click data to be stored
     */
    fun logClick(key: String, data: ClickProperties)


    /**
     * Given the key of a redirection endpoint, it returns a list of all the clicks that have been made to that
     * endpoint.
     *
     * @param hash Hash of the redirection endpoint
     * @return List of clicks for the given hash. If there are no redirections, the list will be empty
     * @throws RedirectionNotFound if the hash does not correspond to any redirection
     */
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
