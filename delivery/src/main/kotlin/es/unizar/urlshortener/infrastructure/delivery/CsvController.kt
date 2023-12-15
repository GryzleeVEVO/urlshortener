@file:Suppress("WildcardImport","LongMethod","CyclomaticComplexMethod")
package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.usecases.CsvUseCase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import jakarta.servlet.http.HttpServletRequest
import org.springframework.hateoas.server.mvc.linkTo

import es.unizar.urlshortener.core.ClickProperties
import org.springframework.web.bind.annotation.PathVariable
import java.net.URI
import es.unizar.urlshortener.core.*

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
@RestController
class CsvControllerImpl(
    private val csvUseCase: CsvUseCase
) : CsvController {

    @PostMapping("/api/bulk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun generateCsv(
        @RequestPart("file") csvFile: MultipartFile,
        @RequestPart("customText") customText: String?,
        request: HttpServletRequest
        
    ): ResponseEntity<String> {
        val ip = request.remoteAddr
        println("Direccion IP del cliente: $ip")

        // Convertir MultipartFile a lista de strings
        val csvContent = readUrlsFromCsv(csvFile).toMutableList()
        val csvContentCopy = csvContent

        //extraer y analizar las cabeceras
        //val validHeaders = setOf("URI", "CUSTOM")
        println("Cabeceras del CSV:")
        csvContentCopy.take(1).forEach { line ->
            val headers = line.split(",")
            println(headers)
            if (headers[0] != "URI" && headers[1] != "Custom_Word"){
                println("Las cabeceras no son correctas")
                throw CsvWrongHeaders("URI,Custom_Word")
            }
            println("Las cabeceras estan bien")
        }


        //ver si el numero de columnas es el correcto
        val validColumnCounts = setOf(1, 2)
        val invalidLines = csvContent.filter { it.split(",").size !in validColumnCounts }
        if (invalidLines.isNotEmpty()) {
            println("ERROR: Hay más o menos columnas de las esperadas")
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

        println("Primera componente del csv: ${csvContent[0]}")

        //separar urls de customWord (si contiene palabras personalizadas)
        val csvUrls = csvContent.map { it.split(",")[0] }
        val csvWords = csvContent.map { if (it.contains(",")) it.split(",")[1] else "" }
        println("URLs: $csvUrls")
        println("Palabras: $csvWords")

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
        
        //ESTE ES EL BUENO (no quita los hash ya usados), poner la ip a los hash
       /*val csvContentWithFullUrls = cadenaResultado.lines().map { line ->
            val splitLine = line.split(",")
            if (splitLine.size >= 2) {
                val (originalUrl, processedUrl) = splitLine
                val fullUrl = linkTo<UrlShortenerControllerImpl> { redirectTo(processedUrl, request) }.toUri()
                "$originalUrl,$fullUrl"
            } else {
                // Handle the case where the line does not contain the expected format
                "Invalid line format: $line"
            }
        }.joinToString("\n")
        */

        // ESTE ES EL BUENO (quita los hash que ya se han usado)
        var encontrada = false
        var primeraLinea = true
        var firstShortenedUrl: String = ""
        val csvContentWithFullUrls = cadenaResultado.lines().map { line ->
            val splitLine = line.split(",")
            if (splitLine.size >= 2) {
                if (primeraLinea){
                    primeraLinea = false
                    "URI,URI_Recortada,Mensaje"
                }
                else{
                    val (originalUrl, processedUrl, errorProcessing) = splitLine

                    val fullUrl = if (errorProcessing == "ALREADY_EXISTS" || errorProcessing == "WRONG_FORMAT" ) {
                        ""
                    } else {
                        linkTo<UrlShortenerControllerImpl> { redirectTo(processedUrl, request) }.toUri()
                    }
                    
                    var mensajeError = ""
                    if (errorProcessing == "ALREADY_EXISTS"){
                        mensajeError = "the custom word [$processedUrl] is already in use"
                    } else if (errorProcessing == "WRONG_FORMAT") {
                        mensajeError = "must be an http or https URI"
                    }else if (!encontrada){ //si no tiene error esa linea
                        encontrada = true
                        //coger la primera url acortada para la cabecera Location
                        println("Primera URL acortada: $fullUrl")
                        firstShortenedUrl = "$fullUrl"
                    }

                    "$originalUrl,$fullUrl,$mensajeError"
                }
            } else {
                // Handle the case where the line does not contain the expected format
                ""
            }
        }.joinToString("\n")

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
        return ResponseEntity(csvContentWithFullUrls, headers, HttpStatus.CREATED)

         


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
