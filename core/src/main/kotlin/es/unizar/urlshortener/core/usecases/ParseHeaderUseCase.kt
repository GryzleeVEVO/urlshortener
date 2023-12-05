package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickProperties
import ua_parser.Client
import ua_parser.Parser


interface ParseHeaderUseCase {
    fun parseHeader(userAgent: String, data: ClickProperties = ClickProperties()): ClickProperties
}


class ParseHeaderUseCaseImpl : ParseHeaderUseCase {
    override fun parseHeader(userAgent: String, data: ClickProperties): ClickProperties {
        val c: Client = Parser().parse(userAgent)

        return ClickProperties(
            ip = data.ip,
            referrer = data.referrer,
            browser = c.userAgent.family,
            platform = c.os.family,
            country = data.country,
        )
    }
}
