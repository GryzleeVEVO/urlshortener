package es.unizar.urlshortener.core

class InvalidUrlException(url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(key: String) : Exception("[$key] is not known")

class UnsafeUrlException(url: String) : Exception("[$url] is not safe")

class UsedCustomWordException(url: String) : Exception("the custom word is already in use")
