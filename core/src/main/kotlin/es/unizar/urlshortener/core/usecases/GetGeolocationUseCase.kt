package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.Click
import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.GeolocationService

interface GetGeolocationUseCase {
    fun getGeolocation(ip: String, data: ClickProperties = ClickProperties()): ClickProperties
}

class GetGeolocationUseCaseImpl(
    private val geolocationService: GeolocationService
) : GetGeolocationUseCase {
    override fun getGeolocation(ip: String, data: ClickProperties): ClickProperties {
        val country = geolocationService.getCountry(ip)

        return ClickProperties(
            ip = data.ip,
            referrer = data.referrer,
            browser = data.browser,
            platform = data.platform,
            country = country,
        )
    }
}
