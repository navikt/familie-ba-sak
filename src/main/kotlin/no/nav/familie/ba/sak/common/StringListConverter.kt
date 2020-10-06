package no.nav.familie.ba.sak.common

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class StringListConverter : AttributeConverter<List<String>, String> {

    override fun convertToDatabaseColumn(stringList: List<String>): String {
        return java.lang.String.join(SPLIT_CHAR, stringList)
    }

    override fun convertToEntityAttribute(string: String): List<String> {
        return string.split(SPLIT_CHAR)
    }

    companion object {

        private const val SPLIT_CHAR = ";"
    }
}
