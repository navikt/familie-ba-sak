package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Service
class SaksstatistikkConverterService(val personopplysningerService: PersonopplysningerService) {

    fun konverterSakTilSisteKontraktVersjon(json: JsonNode): SakDVH {

        return SakDVH(
            funksjonellTid = json.path("funksjonellTid").asZonedDateTime(),
            tekniskTid = json.path("tekniskTid").asZonedDateTime(),
            opprettetDato = json.path("opprettetDato").asLocalDate()!!,
            funksjonellId = funksjonellId(json),
            sakId = json.path("sakId").asText(),
            aktorId = json.path("aktorId").asLong(),
            bostedsland = bostedsLand(json),
            aktorer = (json.path("aktorer") as ArrayNode).map {
                AktørDVH(
                    it.path("aktorId").asLong(),
                    it.path("rolle").asText()
                )
            },
            sakStatus = json.path("sakStatus").asText(),
            avsender = json.path("avsender").asText(),
            versjon = nyesteKontraktversjon(),
            ytelseType = "BARNETRYGD"
        )


    }

    private fun JsonNode.asZonedDateTime(): ZonedDateTime {
        return ZonedDateTime.parse(asText())
    }

    private fun JsonNode.asLocalDate(): LocalDate? {
        return if (asText("").isNotEmpty()) LocalDate.parse(asText()) else null
    }

    private fun funksjonellId(json: JsonNode): String {
        return if (json.path("funksjonellId").asText("").isNotEmpty()) json.path("funksjonellId").asText() else UUID.randomUUID()
            .toString()
    }

    private fun bostedsLand(json: JsonNode): String {
        return if (json.path("bostedsland").asText("").isNotEmpty()) {
            json.path("bostedsland").asText()
        } else {
            hentLandkode(json.path("aktorId").asText())
        }
    }

    private fun hentLandkode(ident: String): String {
        val personInfo = personopplysningerService.hentPersoninfo(ident)

        return if (personInfo.bostedsadresser.isNotEmpty()) "NO" else {
            personopplysningerService.hentLandkodeUtenlandskBostedsadresse(ident)
        }
    }

    companion object {

        fun nyesteKontraktversjon(): String {
            return Utils.hentPropertyFraMaven("familie.kontrakter.saksstatistikk")
                ?: error("Fant ikke nyeste versjonsnummer for kontrakt")
        }
    }

}