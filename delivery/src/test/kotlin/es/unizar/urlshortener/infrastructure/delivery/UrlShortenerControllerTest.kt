@file:Suppress("WildcardImport","MaxLineLength")

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
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart

@WebMvcTest
@ContextConfiguration(
    classes = [
        UrlShortenerControllerImpl::class,
        CsvControllerImpl::class,
        RestResponseEntityExceptionHandler::class
    ]
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
    private lateinit var qrUseCase: QrUseCase

    @MockBean
    private lateinit var parseHeaderUseCase: ParseHeaderUseCase

    @MockBean
    private lateinit var geolocationUseCase: GetGeolocationUseCase

    @MockBean
    private lateinit var csvUseCase: CsvUseCase

    //Test csv
    @Test
    fun `test CSV generation`() {
        //Cargar el contenido del CSV
        val csvContent = "URI,Custom_Word\nhttps://www.google.com/maps,Mapas"
        val csvFile = MockMultipartFile("file", "ShortUrlCollection.csv", "text/csv", csvContent.toByteArray())

        val csvContentList = listOf("https://www.google.com/maps")
        val customWordsList = listOf("Mapas")

        given(csvUseCase.createCsv(
                csvContent = csvContentList,
                customWords = customWordsList,
                ipParam = "127.0.0.1"
            )
        ).willReturn(
            "\"https://www.google.com/maps\",\"Mapas\",\"\"\n"
        )

        //Solicitud y verificacion de la respuesta
        mockMvc.perform(
                multipart("/api/bulk")
                        .file(csvFile)
                        .param("customText", "")
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", "http://localhost/Mapas"))
            .andExpect(content().contentType(MediaType.parseMediaType("text/csv")))
            .andExpect(content().string("URI,URI_Recortada,Mensaje,URI_Qr\nhttps://www.google.com/maps,http://localhost/Mapas,,http://localhost/Mapas/qr\n"))

        verify(csvUseCase).createCsv(csvContentList, customWordsList, "127.0.0.1")
    }

    //Test customWord
    @Test
    fun `creates returns a basic redirect if it can compute a custom word`() {
        given(
                createShortUrlUseCase.create(
                        url = "http://example.com/",
                        data = ShortUrlProperties(ip = "127.0.0.1"),
                        customText = "Example"
                )
        ).willReturn(ShortUrl("Example", Redirection("http://example.com/")))

        mockMvc.perform(
                post("/api/link")
                        .param("url", "http://example.com/")
                        .param("customText","Example")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
                .andDo(print())
                .andExpect(status().isCreated)
                .andExpect(redirectedUrl("http://localhost/Example"))
                .andExpect(jsonPath("$.url").value("http://localhost/Example"))
    }

    @Test
    fun `returns 400 when a custom word is already in use`() {
        given(
                createShortUrlUseCase.create(
                        url = "http://example.com/",
                        data = ShortUrlProperties(ip = "127.0.0.1"),
                        customText = "Example"
                )
        ).willAnswer{throw UsedCustomWordException("Example")}

        mockMvc.perform(
                post("/api/link")
                        .param("url", "http://example.com/")
                        .param("customText", "Example")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
                .andDo(print())
                .andExpect(status().isBadRequest)
    }


    //@Test
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

    //@Test
    fun `redirectTo returns a redirect when the key exists, and there is User-Agent and geolocation info`() {
        // Mock user-agent obtained from https://deviceatlas.com/blog/list-of-user-agent-strings
        // More User-Agents https://www.whatismybrowser.com/guides/the-latest-user-agent/windows
        val mockUserAgent =
            "Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/95.0.4638.54 Mobile Safari/537.36"

        given(redirectUseCase.redirectTo("key"))
            .willReturn(Redirection("http://example.com/"))
        given(parseHeaderUseCase.parseHeader(mockUserAgent, ClickProperties(ip = "155.210.33.10")))
            .willReturn(ClickProperties(ip = "155.210.33.10", browser = "Safari", platform = "Mac OS X"))
        given(geolocationUseCase.getGeolocation("155.210.33.10",
            ClickProperties(ip = "155.210.33.10", browser = "Safari", platform = "Mac OS X")))
            .willReturn(ClickProperties(
                ip = "155.210.33.10", browser = "Safari", platform = "Mac OS X", country = "Spain"))

        mockMvc.perform(get("/{id}", "key")
            .remoteAddress("155.210.33.10")
            .header("User-Agent", mockUserAgent)).andExpect(status().isTemporaryRedirect)
            .andExpect(redirectedUrl("http://example.com/"))

        verify(logClickUseCase).logClick(
            "key", ClickProperties(ip = "155.210.33.10", browser = "Safari", platform = "Mac OS X", country = "Spain")
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
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1"),
                customText = ""
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("customText","")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andDo(print())
            .andExpect(status().isCreated)
            .andExpect(redirectedUrl("http://localhost/f684a3c4"))
            .andExpect(jsonPath("$.url").value("http://localhost/f684a3c4"))
    }

    @Test
    fun `creates returns bad request if it can compute a hash`() {
        given(
            createShortUrlUseCase.create(
                url = "ftp://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1"),
                customText = ""
            )
        ).willAnswer { throw InvalidUrlException("ftp://example.com/") }

        mockMvc.perform(
            post("/api/link")
                .param("url", "ftp://example.com/")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
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
                .andExpect(jsonPath("$.clicks[0].country").doesNotExist())
                .andExpect(jsonPath("$.clicks[1].ip").value("155.210.157.11"))
                .andExpect(jsonPath("$.clicks[1].browser").value("Safari"))
                .andExpect(jsonPath("$.clicks[1].platform").value("Mac OS X"))
                .andExpect(jsonPath("$.clicks[1].country").value("Spain"))
                .andExpect(jsonPath("$.clicks[2].ip").value("97.11.254.3"))
                .andExpect(jsonPath("$.clicks[2].browser").value("Firefox Mobile"))
                .andExpect(jsonPath("$.clicks[2].platform").value("Android"))
                .andExpect(jsonPath("$.clicks[2].country").value("France"))
    }


    @Test
    fun `creates qr code when option is checked`() {
       given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", qr = true),
                customText = ""
            )
       ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("qr", "true")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.properties.qr").value("http://localhost/f684a3c4/qr"))


    }

    @Test
    fun `does not create qr code when option not checked`() {
        given(
            createShortUrlUseCase.create(
                url = "http://example.com/",
                data = ShortUrlProperties(ip = "127.0.0.1", qr = false),
                customText = ""
            )
        ).willReturn(ShortUrl("f684a3c4", Redirection("http://example.com/")))

        mockMvc.perform(
            post("/api/link")
                .param("url", "http://example.com/")
                .param("qr", "false")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        ).andDo(print())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.properties.qr").doesNotExist())




    }
    @Test
    fun `getQrCode returns NOT_FOUND if key does not exist`() {

        given(qrUseCase.canGenerateQrCode("a1b2c3d4")).willReturn(false)

        mockMvc.perform(get("/{id}/qr", "a1b2c3d4"))
                .andDo(print())
                .andExpect(status().isNotFound)
    }

    @Test
    fun `getQrCode returns OK if key does exist`() {

        given(qrUseCase.canGenerateQrCode("a1b2c3d4")).willReturn(true)
        mockMvc.perform(get("/{id}/qr", "a1b2c3d4"))
                .andDo(print())
                .andExpect(status().isOk)
    }

    @Test
    fun `getQrCode returns Forbidden if key exists but qr was not checked`() {

        given(
            qrUseCase.canGenerateQrCode("f684a3c4")
        ).willAnswer { throw QrCodeNotFound("f684a3c4") }

        // Performing the GET request for the QR code and expecting 403 Forbidden
        mockMvc.perform(
            get("/f684a3c4/qr")
                .contentType(MediaType.IMAGE_PNG)
        ).andDo(print())
            .andExpect(status().isForbidden)
    }
}
