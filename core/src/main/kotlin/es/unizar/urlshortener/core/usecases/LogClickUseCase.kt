package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.ClickRepositoryService

/**
 * Log that somebody has requested the redirection identified by a key.
 *
 * **Note**: This is an example of functionality.
 */
    interface LogClickUseCase {
    fun logClick(key: String, data: ClickProperties)

    // TODO
    fun getClicksById()
}

/**
 * Implementation of [LogClickUseCase].
 */
class LogClickUseCaseImpl(
    private val clickRepository: ClickRepositoryService
) : LogClickUseCase {
    override fun logClick(key: String, data: ClickProperties) {
        val cl = Click(
            hash = key,
            properties = data
        )
        clickRepository.save(cl)
    }

    override fun getClicksById() {
        // TODO: Devuelve una lista de clicks realizados para una URL
    }
}
