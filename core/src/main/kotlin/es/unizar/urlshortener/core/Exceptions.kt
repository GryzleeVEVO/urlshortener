package es.unizar.urlshortener.core

class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class UnsafeUrlException(url: String) : Exception("[$url] is not safe")

class UsedCustomWordException(customText: String) : Exception("the custom word [$customText] is already in use")

class CsvColumnsNotExpected(number: String): Exception("the number of columns is not expected, [$number] expected")

class CsvWrongHeaders(headers: String): Exception("the headers are incorrect, they must be [$headers]")

class CsvNotEnoughRows(message: String) : RuntimeException(message)

class QrCodeNotFound(message: String) : RuntimeException(message)
