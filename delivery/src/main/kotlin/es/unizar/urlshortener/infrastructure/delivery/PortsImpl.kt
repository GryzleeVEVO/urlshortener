package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorService
import org.apache.commons.validator.routines.UrlValidator
import java.nio.charset.StandardCharsets

import es.unizar.urlshortener.core.CsvService
import com.opencsv.CSVReader
import java.io.File
import java.io.FileReader

/**
 * Implementation of the port [ValidatorService].
 */
class ValidatorServiceImpl : ValidatorService {
    override fun isValid(url: String) = urlValidator.isValid(url)

    companion object {
        val urlValidator = UrlValidator(arrayOf("http", "https"))
    }
}

/**
 * Implementation of the port [HashService].
 */
class HashServiceImpl : HashService {
    // VERSION DEFAULT
    // override fun hasUrl(url: String) = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
    
    //VERSION BUENA
    override fun hasUrl(url: String, customText: String): String { //, prueba: String
        // val baseHash = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
        // val regex = Regex("[a-zA-Z0-9]+")
        if (customText == ""){ //|| !customText.matches(regex)
            val modifiedHash = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
            return modifiedHash
        }else{
            val modifiedHash = customText
            return modifiedHash           
        }
    }

    // MI VERSION AÑADIENDO A LA URL EL TEXTO "EJEMPLO"
    // override fun hasUrl(url: String): String {
    //     val baseHash = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
    //     val modifiedHash = baseHash + "ejemplo"
    //     return modifiedHash
    // }

    // MI VERSION SOLO EJEMPLO EN LA URL
    // override fun hasUrl(url: String): String {
        //val baseHash = Hashing.murmur3_32_fixed().hashString(url, StandardCharsets.UTF_8).toString()
    //     val modifiedHash = "ejemplo"
    //     return modifiedHash
    // }
}

class CsvServiceImpl: CsvService {
    override fun csvHasUrl(csvFile: File, customText: String): List<String>{

        // Lista dinámica para almacenar el resultado
        val processedUrls = mutableListOf<String>()

        // Utilizar CSVReader de OpenCSV para leer el archivo CSV
        CSVReader(FileReader(csvFile)).use { csvReader ->
            var nextRecord: Array<String>?

            // Leer cada línea del CSV
            while (csvReader.readNext().also { nextRecord = it } != null) {
                // Cogemos el valor de la columna 0
                val url = nextRecord?.get(0)
                if (url != null) {
                    // Instancia de hasServiceImp
                    val hashServiceInstance = HashServiceImpl()
                    // Procesar la URL con la funcion hasUrl 
                    //(por ahora no se tiene en cuenta el custom, habria que tener una lista de string)
                    val result = hashServiceInstance.hasUrl(url, "") //customText
                    
                    // Agregar el resultado a la lista de URLs procesadas
                    processedUrls.add(result)
                }
            }
        }

        // Devolver la lista de URLs procesadas
        return processedUrls
    }
}
