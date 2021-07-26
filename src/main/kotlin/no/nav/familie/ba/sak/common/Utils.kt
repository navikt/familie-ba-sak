package no.nav.familie.ba.sak.common

import no.nav.familie.kontrakter.felles.objectMapper
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.text.NumberFormat
import java.util.*

val nbLocale = Locale("nb", "Norway")

object Utils {

    fun slåSammen(values: List<String>): String = Regex("(.*),").replace(values.joinToString(", "), "$1 og")

    fun formaterBeløp(beløp: Int): String = NumberFormat.getNumberInstance(nbLocale).format(beløp)

    fun hentPropertyFraMaven(key: String): String? {
        val reader = MavenXpp3Reader()
        val model: Model = if (File("pom.xml").exists()) reader.read(FileReader("pom.xml")) else reader.read(
                InputStreamReader(
                        ClassPathResource(
                                "META-INF/maven/no.nav.familie.ba.sak/familie-ba-sak/pom.xml"
                        ).inputStream
                )
        )

        return model.properties[key]?.toString()
    }

    fun String.storForbokstav() = this.lowercase().replaceFirstChar { it.uppercase() }
    fun String.storForbokstavIHvertOrd() = this.split(" ").joinToString(" ") { it.storForbokstav() }.trimEnd()
    fun Any?.nullableTilString() = this?.toString() ?: ""
}

fun Any.convertDataClassToJson(): String {
    return objectMapper.writeValueAsString(this)
}