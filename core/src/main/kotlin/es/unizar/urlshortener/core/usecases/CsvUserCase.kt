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
interface CsvUserCase {
    fun createCsv(csvFile: File, customText: String): String
}

class CsvUserCaseImpl(
    private val csvService: CsvService
) : CsvUserCase {
    override fun createCsv(csvFile: File, customText: String): String {
        // Obtener una lista<string> con las url originales
        val originalUrls = mutableListOf<String>()
        CSVReader(FileReader(csvFile)).use { csvReader ->
            var nextRecord: Array<String>?

            // Leer cada línea del CSV
            while (csvReader.readNext().also { nextRecord = it } != null) {
                // Cogemos el valor de la columna 0
                val url = nextRecord?.get(0)
                if (url != null) {
                    // Agregar el resultado a la lista de URLs procesadas
                    originalUrls.add(url)
                }
            }
        }

        // Obtener una lista<string> con las url recortadas
        val processedUrls = csvService.csvHasUrl(csvFile, customText)

        
        val stringWriter = StringWriter()
        CSVWriter(stringWriter).use { csvWriter ->
            originalUrls.zip(processedUrls).forEach { (originalUrl, processedUrl) ->
                csvWriter.writeNext(arrayOf(originalUrl, processedUrl))
            }
        }

        return stringWriter.toString()
    }
}
