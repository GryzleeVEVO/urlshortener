package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.usecases.CsvUserCase
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


interface CsvController {

    /**
     * Genera y devuelve un csv en funcion del csv de entrada y del texto personalizado(aun no)
     */
    //@RequestPart("customText") customText: String = ""
    fun generateCsv(
    @RequestPart("csvFile") csvFile: MultipartFile,
    @RequestPart("customText") customText: String?
): ResponseEntity<String>
}

/**
 * Implementacion del controlador csv
 */
@RestController
class CsvControllerImpl(
    private val csvUserCase: CsvUserCase
) : CsvController {
    @PostMapping("/api/bulk", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun generateCsv(
        @RequestPart("file") csvFile: MultipartFile,
        @RequestPart("customText") customText: String?
    ): ResponseEntity<String> {
        // Convertir MultipartFile a lista de strings
        val csvContent = readUrlsFromCsv(csvFile)

        // Llamar al caso de uso para generar el CSV
        val csvContentResult = csvUserCase.createCsv(csvContent, "") //customText

        // Configurar la respuesta HTTP
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType("text/csv")
        headers.setContentDispositionFormData("attachment", "output.csv")

        // Devolver la respuesta HTTP con el contenido del CSV
        return ResponseEntity(csvContentResult, headers, HttpStatus.OK)
    }

    // Método para convertir MultipartFile a lista de strings
    private fun readUrlsFromCsv(csvFile: MultipartFile): List<String> {
        return csvFile.inputStream.bufferedReader().readLines()
    }
}
