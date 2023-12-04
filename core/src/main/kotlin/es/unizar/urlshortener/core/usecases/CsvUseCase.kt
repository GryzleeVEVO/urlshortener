@file:Suppress("WildcardImport")

package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

import com.opencsv.CSVReader
import java.io.File
import java.io.FileReader
import com.opencsv.CSVWriter
import java.io.FileWriter
import java.io.StringWriter


/*
 * Dada una lista de customText (POR AHORA ESO NO) y un fichero csv en el que cada 
 * fila de la primera columna es una URL se devuelve un string con las URL acortadas
 * Cuando se crean, se pueden agregar datos opcionales (data)
 */
interface CsvUseCase {
    fun createCsv(csvContent: List<String>, customText: String): String
}

class CsvUseCaseImpl(
    private val csvService: CsvService
) : CsvUseCase {
    override fun createCsv(csvContent: List<String>, customText: String): String {
        // Obtener una lista<string> con las URL originales
        val originalUrls = mutableListOf<String>()
        csvContent.forEach { url ->
            // Agregar el resultado a la lista de URLs procesadas
            originalUrls.add(url)
        }

        // Obtener una lista<string> con las URL recortadas
        val processedUrls = csvService.csvHasUrl(csvContent, customText)

        val stringWriter = StringWriter()
        CSVWriter(stringWriter).use { csvWriter ->
            originalUrls.zip(processedUrls).forEach { (originalUrl, processedUrl) ->
                csvWriter.writeNext(arrayOf(originalUrl, processedUrl))
            }
        }

        return stringWriter.toString()
    }
}

