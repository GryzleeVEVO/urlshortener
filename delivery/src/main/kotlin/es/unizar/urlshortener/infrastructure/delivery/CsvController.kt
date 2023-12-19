@file:Suppress("WildcardImport","LongMethod","CyclomaticComplexMethod")
package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.CsvColumnsNotExpected
import es.unizar.urlshortener.core.CsvNotEnoughRows
import es.unizar.urlshortener.core.CsvWrongHeaders
import es.unizar.urlshortener.core.usecases.CsvUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URI

interface CsvController {

    /**
     * Genera y devuelve un csv en funcion del csv de entrada y del texto personalizado
     */
    fun generateCsv(
        @RequestPart("csvFile") csvFile: MultipartFile,
        @RequestPart("customText") customText: String?,
        request: HttpServletRequest    
    ): ResponseEntity<String>
}

/**
 * Implementacion del controlador csv
 */
@Tag(name = "CSV Controller", description = "The bulk shortener controller")
@RestController
class CsvControllerImpl(
    private val csvUseCase: CsvUseCase
) : CsvController {

    @Operation(summary = "Read a CSV file with links and extra parameters and receive a CSV with shortened links")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "URLs shortened successfully"),
            ApiResponse(responseCode = "400", description = "CSV file could not be processed"),
        ]
    )
    @PostMapping("/api/bulk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun generateCsv(
        @RequestPart("file") csvFile: MultipartFile,
        @RequestPart("customText") customText: String?,
        request: HttpServletRequest
        
    ): ResponseEntity<String> {
        val ip = request.remoteAddr
        //println("Direccion IP del cliente: $ip")

        // Convertir MultipartFile a lista de strings
        val csvContent = readUrlsFromCsv(csvFile).toMutableList()
        val csvContentCopy = csvContent

        // ver si hay al menos 2 lineas
        if (csvContent.size < 2) {
            throw CsvNotEnoughRows("The csv file must have two or more lines")
        }

        //extraer y analizar las cabeceras
        //val validHeaders = setOf("URI", "CUSTOM")
        //println("Cabeceras del CSV:")
        csvContentCopy.take(1).forEach { line ->
            val headers = line.split(",")
            //println(headers)
            if (headers[0] != "URI" && headers[1] != "Custom_Word"){
                //println("Las cabeceras no son correctas")
                throw CsvWrongHeaders("URI,Custom_Word")
            }
            //println("Las cabeceras estan bien")
        }


        //ver si el numero de columnas es el correcto
        val validColumnCounts = setOf(1, 2)
        val invalidLines = csvContent.filter { it.split(",").size !in validColumnCounts }
        if (invalidLines.isNotEmpty()) {
            //println("ERROR: Hay más o menos columnas de las esperadas")
            // throw InvalidUrlException("fdfsj")
            //ResponseEntity("CSV con más columnas de las permitidas", HttpStatus.BAD_REQUEST)
            throw CsvColumnsNotExpected("2")
        }
        
        // quitar cabeceras para el procesado
        for (j in csvContent.indices){
            if (j > 0){
                csvContent[j-1] = csvContent[j]
            }
        }
        csvContent.removeAt(csvContent.size - 1)

        //println("Primera componente del csv: ${csvContent[0]}")

        //separar urls de customWord (si contiene palabras personalizadas)
        val csvUrls = csvContent.map { it.split(",")[0] }
        val csvWords = csvContent.map { if (it.contains(",")) it.split(",")[1] else "" }
        //println("URLs: $csvUrls")
        //println("Palabras: $csvWords")

        // Llamar al caso de uso para generar el CSV
        val csvContentResult = csvUseCase.createCsv(csvUrls, csvWords, ip) //customText

        // eliminar las comillas innecesarias
        var cadenaResultado = ""
        for (i in csvContentResult.indices){
            if (csvContentResult[i].toString() != "\""){
                cadenaResultado+=csvContentResult[i]
            }
        }
        // println("csvContentResult:")
        // for (item in csvContentResult) {
        //     println(item)
        // }
        // println("cadenaResultado:")
        // for (item in cadenaResultado) {
        //     println(item)
        // }


        // ESTE ES EL BUENO (quita los hash que ya se han usado)
        var encontrada = false
        var firstShortenedUrl: String = ""
        val csvContentWithFullUrls = cadenaResultado.lines().map { line ->
            val splitLine = line.split(",")
            if (splitLine.size >= 2) {
                val (originalUrl, processedUrl, errorProcessing) = splitLine

                //println("comprobando url: $originalUrl, $processedUrl")
                val fullUrl = if (errorProcessing == "ALREADY_EXISTS" || errorProcessing == "WRONG_FORMAT" ) {
                    ""
                } else {
                    //println("genrando url: $originalUrl, $processedUrl")
                    linkTo<UrlShortenerControllerImpl> { redirectTo(processedUrl, request) }.toUri()
                }

                var mensajeError = ""
                var urlQr = ""
                if (errorProcessing == "ALREADY_EXISTS"){
                    mensajeError = "the custom word [$processedUrl] is already in use"
                } else if (errorProcessing == "WRONG_FORMAT") {
                    mensajeError = "must be an http or https URI"
                }else if (!encontrada){ //si no tiene error esa linea
                    encontrada = true
                    //coger la primera url acortada para la cabecera Location
                    //println("Primera URL acortada: $fullUrl")
                    firstShortenedUrl = "$fullUrl"
                    urlQr = "$fullUrl/qr"
                }else{
                    urlQr = "$fullUrl/qr"
                }


                "$originalUrl,$fullUrl,$mensajeError,$urlQr"
            } else {
                // Handle the case where the line does not contain the expected format
                ""
            }
        }.joinToString("\n")
        val csvContentWithHeader = "URI,URI_Recortada,Mensaje,URI_Qr\n$csvContentWithFullUrls"

        //(ESTO AHORA ESTA DENTRO DEL CODIGO DE ARRIBA DONDE SE PONE EL TIPO DE ERROR, 
        //  SI NO HAY ERROR ES QUE LA URL ACORTADA ES VALIDA Y SE COGE A PRIMERA) 
        //coger la primera url acortada para la cabecera location 
        // val lines = csvContentWithFullUrls.trim().split("\n")
        // var firstShortenedUrl: String = ""
        // for (line in lines) {
        //     firstShortenedUrl = line.substringAfter(",")
        //     println("Primera URL acortada: $firstShortenedUrl")
        //     break
        // }

        // Configurar la respuesta HTTP
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType("text/csv")
        headers.setContentDispositionFormData("attachment", "ShortUrlCollection.csv")
        headers.location = URI.create(firstShortenedUrl)
            // Devolver la respuesta HTTP con el contenido del CSV
        return ResponseEntity(csvContentWithHeader, headers, HttpStatus.CREATED)

         


        // Configurar la respuesta HTTP
        ///val headers = HttpHeaders()
        //headers.contentType = MediaType.parseMediaType("text/csv")
        //headers.setContentDispositionFormData("attachment", "output.csv")

        // Devolver la respuesta HTTP con el contenido del CSV
        //return ResponseEntity(csvContentResult, headers, HttpStatus.CREATED) //HTTP: 201  
    }

    // Método para convertir MultipartFile a lista de strings
    private fun readUrlsFromCsv(csvFile: MultipartFile): List<String> {
        return csvFile.inputStream.bufferedReader().readLines()
    }
}
