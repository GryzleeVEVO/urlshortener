package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.GeolocationService


/**
 * [GetGeolocationUseCase] offers geolocation functionality for an HTTP request. Given an IP, it returns the
 * [country][ClickProperties.country] of origin of the request, if available.
 */
interface GetGeolocationUseCase {

    /**
     * Given the IP of a request, it obtains and adds to [data], if available, the [country][ClickProperties.country]
     * of origin of the request.
     *
     * @param ip IP of the remote host
     * @param data A [ClickProperties] object
     * @return [ClickProperties] object equal to data with the [country][ClickProperties.country] field added.
     */
    fun getGeolocation(ip: String, data: ClickProperties = ClickProperties()): ClickProperties
}

/**
 * Implementation of [GetGeolocationUseCase].
 */
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
