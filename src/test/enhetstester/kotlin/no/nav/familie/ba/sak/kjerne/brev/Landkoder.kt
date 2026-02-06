package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.kontrakter.felles.jsonMapper
import org.springframework.core.io.ClassPathResource
import tools.jackson.module.kotlin.readValue
import java.io.BufferedReader

data class LandkodeISO2(
    val code: String,
    val name: String,
)

fun hentLandkoderISO2(): Map<String, String> {
    val landkoder =
        ClassPathResource("landkoder/landkoder.json").inputStream.bufferedReader().use(BufferedReader::readText)

    return jsonMapper
        .readValue<List<LandkodeISO2>>(landkoder)
        .associate { it.code to it.name }
}

val LANDKODER = hentLandkoderISO2()
