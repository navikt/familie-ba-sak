package no.nav.familie.ba.sak.cucumber.domeneparser

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
    rad: Map<String, String>,
): LocalDate = parseDato(domenebegrep.nøkkel, rad)

fun parseValgfriDato(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String?>,
): LocalDate? = parseValgfriDato(domenebegrep.nøkkel, rad)

fun parseÅrMåned(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String?>,
): YearMonth = parseValgfriÅrMåned(domenebegrep.nøkkel, rad)!!

fun parseValgfriÅrMåned(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String?>,
): YearMonth? = parseValgfriÅrMåned(domenebegrep.nøkkel, rad)

fun parseString(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): String = verdi(domenebegrep.nøkkel, rad)

fun parseValgfriString(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): String? = valgfriVerdi(domenebegrep.nøkkel, rad)

fun parseBoolean(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Boolean {
    val verdi = verdi(domenebegrep.nøkkel, rad)

    return when (verdi) {
        "Ja" -> true
        else -> false
    }
}

fun parseValgfriBoolean(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String?>,
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
    rad: Map<String, String>,
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

fun parseValgfriDato(
    domenebegrep: String,
    rad: Map<String, String?>,
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
    rad: Map<String, String?>,
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
    rad: Map<String, String>,
): String {
    val verdi = rad[nøkkel]

    if (verdi == null || verdi == "") {
        throw java.lang.RuntimeException("Fant ingen verdi for $nøkkel")
    }

    return verdi
}

fun valgfriVerdi(
    nøkkel: String,
    rad: Map<String, String>,
): String? = rad[nøkkel]

fun parseInt(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Int {
    val verdi = verdi(domenebegrep.nøkkel, rad).replace("_", "")

    return Integer.parseInt(verdi)
}

fun parseLong(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Long {
    val verdi = verdi(domenebegrep.nøkkel, rad).replace("_", "")

    return verdi.toLong()
}

fun parseList(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): List<Long> = verdi(domenebegrep.nøkkel, rad).split(",").map { it.trim().toLong() }

fun parseStringList(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): List<String> = verdi(domenebegrep.nøkkel, rad).split(",").map { it.trim() }

fun parseValgfriStringList(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): List<String> = valgfriVerdi(domenebegrep.nøkkel, rad)?.split(",")?.map { it.trim() } ?: emptyList()

fun parseBigDecimal(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): BigDecimal {
    val verdi = verdi(domenebegrep.nøkkel, rad)
    return verdi.toBigDecimal()
}

fun parseValgfriLong(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Long? =
    parseValgfriInt(domenebegrep, rad)?.toLong()

fun parseValgfriInt(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): Int? {
    valgfriVerdi(domenebegrep.nøkkel, rad) ?: return null

    return parseInt(domenebegrep, rad)
}

inline fun <reified T : Enum<T>> parseValgfriEnum(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): T? {
    val verdi = valgfriVerdi(domenebegrep.nøkkel, rad) ?: return null
    return enumValueOf<T>(verdi.uppercase())
}

inline fun <reified T : Enum<T>> parseEnum(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): T = parseValgfriEnum<T>(domenebegrep, rad) ?: error("Fant ikke enum verdi for ${domenebegrep.nøkkel}. Gjelder rad $rad")

inline fun <reified T : Enum<T>> parseEnumListe(
    domenebegrep: Domenenøkkel,
    rad: Map<String, String>,
): List<T> {
    val stringVerdier = valgfriVerdi(domenebegrep.nøkkel, rad)?.split(",")?.map { it.trim() } ?: return emptyList()
    return stringVerdier.map {
        enumValueOf<T>(it.uppercase())
    }
}
