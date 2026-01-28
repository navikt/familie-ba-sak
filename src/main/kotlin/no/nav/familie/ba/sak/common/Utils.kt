package no.nav.familie.ba.sak.common

import no.nav.familie.kontrakter.felles.jsonMapper
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import java.util.Properties

val nbLocale = Locale.of("nb", "Norway")

val secureLogger = LoggerFactory.getLogger("secureLogger")

object Utils {
    fun List<String>.slåSammen(): String =
        when (size) {
            0 -> ""
            1 -> first()
            2 -> joinToString(" og ")
            else -> this.dropLast(1).joinToString(", ") + " og ${last()}"
        }

    fun formaterBeløp(beløp: Int): String = NumberFormat.getNumberInstance(nbLocale).format(beløp)

    val properties: Properties by lazy {
        val reader = MavenXpp3Reader()
        val model: Model =
            if (File("pom.xml").exists()) {
                reader.read(FileReader("pom.xml"))
            } else {
                reader.read(
                    InputStreamReader(
                        ClassPathResource(
                            "META-INF/maven/no.nav.familie.ba.sak/familie-ba-sak/pom.xml",
                        ).inputStream,
                    ),
                )
            }
        model.properties
    }

    fun hentPropertyFraMaven(key: String): String? = this.properties[key]?.toString()

    fun BigDecimal.avrundetHeltallAvProsent(prosent: BigDecimal) =
        this
            .times(prosent)
            .divide(100.toBigDecimal())
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()

    fun Int.avrundetHeltallAvProsent(prosent: BigDecimal) = this.toBigDecimal().avrundetHeltallAvProsent(prosent)

    fun String.storForbokstav() = this.lowercase().replaceFirstChar { it.uppercase() }

    fun String.storForbokstavIHvertOrd() = this.split(" ").joinToString(" ") { it.storForbokstav() }.trimEnd()

    fun String.storForbokstavIAlleNavn() =
        this
            .split(" ")
            .joinToString(" ") { navn ->
                navn.split("-").joinToString("-") { it.storForbokstav() }
            }.trimEnd()

    fun Any?.nullableTilString() = this?.toString() ?: ""

    inline fun <reified T : Enum<T>> konverterEnumsTilString(liste: List<T>) = liste.joinToString(separator = ";")

    inline fun <reified T : Enum<T>> konverterStringTilEnums(string: String?): List<T> = if (string.isNullOrBlank()) emptyList() else string.split(";").map { enumValueOf(it) }

    fun <T, R> List<T>.tilEtterfølgendePar(transform: (a: T, b: T?) -> R): List<R> = this.windowed(size = 2, step = 1, partialWindows = true) { transform(it[0], it.getOrNull(1)) }
}

fun Any.convertDataClassToJson(): String = jsonMapper.writeValueAsString(this)
