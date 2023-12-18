package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ClickProperties
import ua_parser.Client
import ua_parser.Parser

/**
 * [ParseHeaderUseCase] offers a parser for an HTTP request's user agent header, returning relevant data form the string
 * such as the [browser][ClickProperties.browser] (or other program a request such as cURL, wget or a crawler bot)
 * and [platform][ClickProperties.platform] (in this case, the operating system of the requesting device).
 */
interface ParseHeaderUseCase {

    /**
     * Given the user agent header from a request, it parses it obtaining and adding to [data], if available, the
     * [browser][ClickProperties.browser] and [platform][ClickProperties.platform] information.
     *
     *
     * @param userAgent User agent header content
     * @param data A [ClickProperties] object
     * @return [ClickProperties] object equal to data with the [browser][ClickProperties.browser] and
     * [platform][ClickProperties.platform] fields added.
     */
    fun parseHeader(userAgent: String?, data: ClickProperties = ClickProperties()): ClickProperties
}


/**
 * Implementation of [ParseHeaderUseCase].
 */
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
