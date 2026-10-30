package no.nav.familie.ba.sak.cucumber.domeneparser

import no.nav.familie.ba.sak.common.Feil
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

val norskDatoFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
val norskÅrMånedFormatter = DateTimeFormatter.ofPattern("MM.yyyy")
val isoDatoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
val isoÅrMånedFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

fun parseDato(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): LocalDate = parseDato(domenebegrep.nøkkel, rad)

fun parseValgfriDato(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): LocalDate? = parseValgfriDato(domenebegrep.nøkkel, rad)

fun parseÅrMåned(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): YearMonth = parseValgfriÅrMåned(domenebegrep.nøkkel, rad)!!

fun parseValgfriÅrMåned(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): YearMonth? = parseValgfriÅrMåned(domenebegrep.nøkkel, rad)

fun parseString(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): String = verdi(domenebegrep.nøkkel, rad)

fun parseValgfriString(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): String? = valgfriVerdi(domenebegrep.nøkkel, rad)

fun parseBoolean(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): Boolean {
    val verdi = verdi(domenebegrep.nøkkel, rad)

    return when (verdi) {
        "Ja" -> true
        else -> false
    }
}

fun parseValgfriBoolean(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): Boolean? {
    val verdi = rad[domenebegrep.nøkkel]
    if (verdi == null || verdi == "") {
        return null
    }

    return when (verdi.uppercase()) {
        "JA" -> true
        "NEI" -> false
        else -> null
    }
}

fun parseDato(
    domenebegrep: String,
    rad: Tabellrad,
): LocalDate {
    val dato = rad[domenebegrep]!!

    return parseDato(dato)
}

fun parseDato(dato: String): LocalDate =
    if (dato.contains(".")) {
        LocalDate.parse(dato, norskDatoFormatter)
    } else {
        LocalDate.parse(dato, isoDatoFormatter)
    }

fun parseValgfriDato(dato: String?): LocalDate? =
    when {
        dato == null || dato == "" -> null
        dato.contains(".") -> LocalDate.parse(dato, norskDatoFormatter)
        else -> LocalDate.parse(dato, isoDatoFormatter)
    }

fun parseValgfriDato(
    domenebegrep: String,
    rad: Tabellrad,
): LocalDate? {
    val verdi = rad[domenebegrep]
    if (verdi == null || verdi == "") {
        return null
    }

    return if (verdi.contains(".")) {
        LocalDate.parse(verdi, norskDatoFormatter)
    } else {
        LocalDate.parse(verdi, isoDatoFormatter)
    }
}

fun parseValgfriÅrMåned(
    domenebegrep: String,
    rad: Tabellrad,
): YearMonth? {
    val verdi = rad[domenebegrep]
    if (verdi == null || verdi == "") {
        return null
    }

    return parseÅrMåned(verdi)
}

fun parseÅrMåned(verdi: String): YearMonth =
    if (verdi.contains(".")) {
        YearMonth.parse(verdi, norskÅrMånedFormatter)
    } else {
        YearMonth.parse(verdi, isoÅrMånedFormatter)
    }

fun verdi(
    nøkkel: String,
    rad: Tabellrad,
): String {
    val verdi = rad[nøkkel]

    if (verdi == null || verdi == "") {
        throw java.lang.RuntimeException("Fant ingen verdi for $nøkkel")
    }

    return verdi
}

fun valgfriVerdi(
    nøkkel: String,
    rad: Tabellrad,
): String? = rad[nøkkel]

fun parseInt(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): Int {
    val verdi = verdi(domenebegrep.nøkkel, rad).replace("_", "")

    return Integer.parseInt(verdi)
}

fun parseLong(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): Long {
    val verdi = verdi(domenebegrep.nøkkel, rad).replace("_", "")

    return verdi.toLong()
}

fun parseList(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): List<Long> = verdi(domenebegrep.nøkkel, rad).split(",").map { it.trim().toLong() }

fun parseStringList(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): List<String> = verdi(domenebegrep.nøkkel, rad).split(",").map { it.trim() }

fun parseValgfriStringList(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): List<String> = valgfriVerdi(domenebegrep.nøkkel, rad)?.split(",")?.map { it.trim() } ?: emptyList()

fun parseBigDecimal(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): BigDecimal {
    val verdi = verdi(domenebegrep.nøkkel, rad)
    return verdi.toBigDecimal()
}

fun parseValgfriBigDecimal(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): BigDecimal? {
    val verdi = valgfriVerdi(domenebegrep.nøkkel, rad)
    return verdi?.toBigDecimal()
}

fun parseValgfriLong(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): Long? = parseValgfriInt(domenebegrep, rad)?.toLong()

fun parseValgfriInt(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): Int? {
    valgfriVerdi(domenebegrep.nøkkel, rad) ?: return null

    return parseInt(domenebegrep, rad)
}

inline fun <reified T : Enum<T>> parseValgfriEnum(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): T? {
    val verdi = valgfriVerdi(domenebegrep.nøkkel, rad) ?: return null
    return enumValueOf<T>(verdi.uppercase())
}

inline fun <reified T : Enum<T>> parseEnum(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): T =
    parseValgfriEnum<T>(domenebegrep, rad)
        ?: throw Feil("Fant ikke enum verdi for ${domenebegrep.nøkkel}. Gjelder rad $rad")

inline fun <reified T : Enum<T>> parseEnumListe(
    domenebegrep: Domenenøkkel,
    rad: Tabellrad,
): List<T> {
    val stringVerdier = valgfriVerdi(domenebegrep.nøkkel, rad)?.split(",")?.map { it.trim() } ?: return emptyList()
    return stringVerdier.map {
        enumValueOf<T>(it.uppercase())
    }
}
