@file:Suppress("WildcardImport")

package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest
@ContextConfiguration(
    classes = [UrlShortenerControllerImpl::class, RestResponseEntityExceptionHandler::class]
)
class UrlShortenerControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var redirectUseCase: RedirectUseCase

    @MockBean
    private lateinit var logClickUseCase: LogClickUseCase

    @MockBean
    private lateinit var createShortUrlUseCase: CreateShortUrlUseCase

    @MockBean
    private lateinit var parseHeaderUseCase: ParseHeaderUseCase

    @MockBean
    private lateinit var geolocationUseCase: GetGeolocationUseCase

    @Test
    fun `redirectTo returns a redirect when the key exists, no User-Agent info and geolocation available`() {
        given(redirectUseCase.redirectTo("key"))
            .willReturn(Redirection("http://example.com/"))
        given(parseHeaderUseCase.parseHeader(null, ClickProperties(ip = "127.0.0.1")))
            .willReturn(ClickProperties(ip = "127.0.0.1"))
        given(geolocationUseCase.getGeolocation("127.0.0.1", ClickProperties(ip = "127.0.0.1")))
            .willReturn(ClickProperties(ip = "127.0.0.1"))

        mockMvc.perform(get("/{id}", "key"))
            .andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `redirectTo returns a redirect when the key exists, and there is User-Agent and geolocation info`() {
        // Mock user-agent obtained from https://deviceatlas.com/blog/list-of-user-agent-strings
        // More User-Agents https://www.whatismybrowser.com/guides/the-latest-user-agent/windows
        val mockUserAgent =
            "Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) " + "Chrome/95.0.4638.54 Mobile Safari/537.36"

        given(redirectUseCase.redirectTo("key"))
            .willReturn(Redirection("http://example.com/"))
        given(parseHeaderUseCase.parseHeader(mockUserAgent, ClickProperties(ip = "127.0.0.1")))
            .willReturn(ClickProperties(ip = "127.0.0.1", browser = "Safari", platform = "Mac OS X"))
        given(geolocationUseCase.getGeolocation("127.0.0.1", ClickProperties(ip = "127.0.0.1", browser = "Safari", platform = "Mac OS X")))
            .willReturn(ClickProperties(ip = "127.0.0.1", browser = "Safari", platform = "Mac OS X", country = "Spain"))

        mockMvc.perform(get("/{id}", "key")
            //.remoteAddress("155.210.33.10")
            .header("User-Agent", mockUserAgent)).andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick(
            "key", ClickProperties(ip = "127.0.0.1", browser = "Safari", platform = "Mac OS X", country = "Spain")
        )
    }

    @Test
    fun `redirectTo returns a not found when the key does not exist`() {
        given(redirectUseCase.redirectTo("key")).willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/{id}", "key")).andDo(print()).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))

        verify(logClickUseCase, never()).logClick("key", ClickProperties(ip = "127.0.0.1"))
    }

    @Test
    fun `creates returns a basic redirect if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/", data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link").param("url", "http://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        ).andDo(print()).andExpect(status().isCreated).andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
    }

    @Test
    fun `creates returns bad request if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "ftp://example.com/", data = ShortUrlProperties(ip = "127.0.0.1")
            )
        ).willAnswer { throw InvalidUrlException("ftp://example.com/") }

        mockMvc.perform(
            post("/api/link").param("url", "ftp://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        ).andExpect(status().isBadRequest).andExpect(jsonPath("$.statusCode").value(400))
    }

    @Test
    fun `statistics returns not found if the key does not exist`() {
        given(logClickUseCase.getClicksByShortUrlHash("key")).willAnswer { throw RedirectionNotFound("key") }

        mockMvc.perform(get("/api/link/{id}", "key")).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))
    }

    @Test
    fun `statistics returns an empty click array if the key exists but there are no clicks`() {
        given(logClickUseCase.getClicksByShortUrlHash("key")).willReturn(emptyList())

        mockMvc.perform(get("/api/link/{id}", "key")).andExpect(status().isOk).andExpect(jsonPath("$.clicks").isArray)
            .andExpect(jsonPath("$.clicks").isEmpty)
    }

    @Test
    fun `statistics returns a list of click properties if the key exists and there are clicks`() {
        // Mock click entries
        // Countries are fake
        // Random IPs obtained with https://www.browserling.com/tools/random-ip
        val clickList: List<ClickProperties> = listOf(
            ClickProperties(ip = "127.0.0.1", browser = "Edge", platform = "Windows", country = null),
            ClickProperties(ip = "155.210.157.11", browser = "Safari", platform = "Mac OS X", country = "Spain"),
            ClickProperties(ip = "97.11.254.3", browser = "Firefox Mobile", platform = "Android", country = "France"),
        )

        given(logClickUseCase.getClicksByShortUrlHash("key")).willReturn(clickList)

        mockMvc.perform(
            get("/api/link/{id}", "key")
        ).andExpect(status().isOk).andExpect(jsonPath("$.clicks").isArray).andExpect(jsonPath("$.clicks").isNotEmpty)
            .andExpect(jsonPath("$.clicks[0].ip").value("127.0.0.1"))
            .andExpect(jsonPath("$.clicks[0].browser").value("Edge"))
            .andExpect(jsonPath("$.clicks[0].platform").value("Windows"))
            .andExpect(jsonPath("$.clicks[0].country").doesNotExist()) // Assuming it should not exist in this case
            .andExpect(jsonPath("$.clicks[1].ip").value("155.210.157.11"))
            .andExpect(jsonPath("$.clicks[1].browser").value("Safari"))
            .andExpect(jsonPath("$.clicks[1].platform").value("Mac OS X"))
            .andExpect(jsonPath("$.clicks[1].country").value("Spain"))
            .andExpect(jsonPath("$.clicks[2].ip").value("97.11.254.3"))
            .andExpect(jsonPath("$.clicks[2].browser").value("Firefox Mobile"))
            .andExpect(jsonPath("$.clicks[2].platform").value("Android"))
            .andExpect(jsonPath("$.clicks[2].country").value("France"))
    }
}
