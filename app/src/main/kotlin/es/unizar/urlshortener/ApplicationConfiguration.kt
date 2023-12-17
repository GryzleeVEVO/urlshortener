@file:Suppress("TooManyFunctions")

package es.unizar.urlshortener

import es.unizar.urlshortener.core.usecases.QrCodeUseCaseImpl
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCaseImpl
import es.unizar.urlshortener.core.usecases.LogClickUseCaseImpl
import es.unizar.urlshortener.core.usecases.ParseHeaderUseCaseImpl
import es.unizar.urlshortener.core.usecases.RedirectUseCaseImpl
import es.unizar.urlshortener.core.usecases.GetGeolocationUseCaseImpl
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.QrCodeServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ClickEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ClickRepositoryServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.GeolocationServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlEntityRepository
import es.unizar.urlshortener.infrastructure.repositories.ShortUrlRepositoryServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import es.unizar.urlshortener.infrastructure.delivery.CsvServiceImpl
import es.unizar.urlshortener.core.usecases.CsvUseCaseImpl

/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository
) {
    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)

    @Bean
    fun validatorService() = ValidatorServiceImpl()

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun geolocationService() = GeolocationServiceImpl()

    @Bean
    fun csvService() = CsvServiceImpl()

    @Bean
    fun qrCodeService() = QrCodeServiceImpl()

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService(), shortUrlRepositoryService())

    @Bean
    fun parseHeaderUseCase() = ParseHeaderUseCaseImpl()

    @Bean
    fun getGeolocationUseCase() = GetGeolocationUseCaseImpl(geolocationService())

    @Bean
    fun qrCodeUseCas() = QrCodeUseCaseImpl(qrCodeService(), shortUrlRepositoryService())


    @Bean
    fun createShortUrlUseCase() =
        CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), hashService())

    @Bean
    fun csvUseCase() =
        CsvUseCaseImpl(shortUrlRepositoryService(), validatorService(), csvService())
}
