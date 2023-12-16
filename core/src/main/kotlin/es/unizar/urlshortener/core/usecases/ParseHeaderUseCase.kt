package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickProperties
import ua_parser.Client
import ua_parser.Parser


interface ParseHeaderUseCase {
    fun parseHeader(userAgent: String?, data: ClickProperties = ClickProperties()): ClickProperties
}


class ParseHeaderUseCaseImpl : ParseHeaderUseCase {
    override fun parseHeader(userAgent: String?, data: ClickProperties): ClickProperties {
        if (userAgent.isNullOrEmpty()) return data

        val c: Client = Parser().parse(userAgent)

        return ClickProperties(
            ip = data.ip,
            referrer = data.referrer,
            browser = if (!c.userAgent.family.isNullOrEmpty() && !c.userAgent.family.equals("Other"))
                c.userAgent.family else null,
            platform = if (!c.os.family.isNullOrEmpty() && !c.os.family.equals("Other"))
                c.os.family else null,
            country = data.country,
        )
    }
}
