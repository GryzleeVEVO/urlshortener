@file:Suppress("WildcardImport","Detekt.NestedBlockDepth")

package es.unizar.urlshortener.core.usecases

import com.opencsv.CSVWriter
import es.unizar.urlshortener.core.*
import java.io.StringWriter


/*
 * Dada una lista de customText y un fichero csv en el que cada 
 * fila de la primera columna es una URL se devuelve un string con las URL acortadas
 * Cuando se crean, se pueden agregar datos opcionales (data)
 */
interface CsvUseCase {
    fun createCsv(csvContent: List<String>, customWords: List<String>, ipParam: String): String
}

class CsvUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val csvService: CsvService
) : CsvUseCase {
    override fun createCsv(csvContent: List<String>, customWords: List<String>, ipParam: String): String {
        // Obtener una lista<string> con las URL originales
        val originalUrls = mutableListOf<String>()
        csvContent.forEach { url ->
            // Agregar el resultado a la lista de URLs procesadas
            originalUrls.add(url)
        }

        //separar la lista de urls de la lista de customWord (mejor hacerlo en el controller)


        // Obtener una lista<string> con las URL recortadas
        val processedUrls = csvService.csvHasUrl(csvContent, customWords)
        
        // ASI NO DEPENDO DE csvHashUrl        
        // Lista dinámica para almacenar el resultado
        // val processedUrls = mutableListOf<String>()
        // for (url in csvContent) {
        //     val processedUrl = hashService.hasUrl(url, "")
        //     processedUrls.add(processedUrl)
        // }

        // lista que indica si ya se ha usado esa palabra para comunacarlo al CsvController
        val errorProcessing = mutableListOf<String>()
        // Guardar en la BD
        for (i in csvContent.indices) {
            //println("Redireccion a: ${csvContent[i]}")
            //println("Hash: ${processedUrls[i]}")
            //println("Direccion IP del cliente: $ipParam")

            
                shortUrlRepository.findByKey(processedUrls[i])?.let{
                    if (customWords[i] != ""){
                        //println("YA ESTA EN LA BDDDDDDDDDDD")
                        //sustituir la url recortada por vacio
                        errorProcessing.add("ALREADY_EXISTS")
                    } else{
                        //println("CustomWord: no tiene custom word")
                        errorProcessing.add("")
                    }
                }?: run {
                    if (validatorService.isValid(csvContent[i])) {
                        //println("NO ESTA EN LA BDDDDDDDDDDD")
                        val su = ShortUrl(
                            hash = processedUrls[i],
                            redirection = Redirection(target = csvContent[i]),
                            properties = ShortUrlProperties(
                                ip = ipParam,
                                qr = true
                            )
                        )  
                        shortUrlRepository.save(su)
                        errorProcessing.add("")
                    } else {
                        //println("No guardado en la BD porque no cumple el formato de url")
                        errorProcessing.add("WRONG_FORMAT")
                    }
                }
            
            
        }

        //println("errorProcessing: $errorProcessing")

        // se ponen de por si comillas en el hash
        val stringWriter = StringWriter()
        CSVWriter(stringWriter).use { csvWriter ->
            // originalUrls.zip(processedUrls).forEach { (originalUrl, processedUrl) ->
            //     csvWriter.writeNext(arrayOf(originalUrl, processedUrl))
            // }
            originalUrls.zip(processedUrls.zip(errorProcessing)).forEach { (originalUrl, pair) ->
                val (processedUrl, alreadyUsedWord) = pair
                csvWriter.writeNext(arrayOf(originalUrl, processedUrl, alreadyUsedWord.toString()))
            }


        }

        return stringWriter.toString()
    }
}

