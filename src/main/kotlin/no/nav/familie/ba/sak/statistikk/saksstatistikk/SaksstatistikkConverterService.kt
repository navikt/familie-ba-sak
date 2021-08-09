package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.eksterne.kontrakter.saksstatistikk.AktørDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.ResultatBegrunnelseDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Service
class SaksstatistikkConverterService(val personopplysningerService: PersonopplysningerService,
                                     val behandlingService: BehandlingService,
                                     val vedtakService: VedtakService,
                                     val totrinnskontrollService: TotrinnskontrollService) {

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

    fun konverterBehandlingTilSisteKontraktVersjon(json: JsonNode): BehandlingDVH {
        val behandlingId = json.path("behandlingId").asText()
        val behandling = behandlingService.hent(behandlingId.toLong())
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)

        return BehandlingDVH(
                funksjonellTid = json.path("funksjonellTid").asZonedDateTime(),
                tekniskTid = json.path("tekniskTid").asZonedDateTime(),
                mottattDato = json.path("mottattDato").asZonedDateTime(),
                registrertDato = json.path("registrertDato").asZonedDateTime(),
                behandlingId = behandlingId,
                funksjonellId = funksjonellId(json),
                sakId = json.path("sakId").asText(),
                behandlingType = json.path("behandlingType").asText(),
                behandlingStatus = json.path("behandlingStatus").asText(),
                behandlingKategori = json.path("behandlingUnderkategori").asText(),
                behandlingUnderkategori = null,
                behandlingAarsak = behandling.opprettetÅrsak.name,
                automatiskBehandlet = behandling.skalBehandlesAutomatisk,
                utenlandstilsnitt = json.path("behandlingKategori").asText(),
                ansvarligEnhetKode = json.path("ansvarligEnhetKode").asText(),
                behandlendeEnhetKode = json.path("behandlendeEnhetKode").asText(),
                ansvarligEnhetType = json.path("ansvarligEnhetType").asText(),
                behandlendeEnhetType = json.path("behandlendeEnhetType").asText(),
                totrinnsbehandling = json.path("totrinnsbehandling").asBoolean(),
                avsender = json.path("avsender").asText(),
                versjon = nyesteKontraktversjon(),
                vedtaksDato = json.path("vedtaksDato").asLocalDate(),
                relatertBehandlingId = json.path("relatertBehandlingId").asText(),
                vedtakId = json.path("vedtakId").asText(),
                resultat = json.path("resultat").asText(),
                resultatBegrunnelser = behandling.resultatBegrunnelser(),
                behandlingTypeBeskrivelse = json.path("behandlingTypeBeskrivelse").asText(),
                behandlingStatusBeskrivelse = json.path("behandlingStatusBeskrivelse")
                        .asText(),
                utenlandstilsnittBeskrivelse = json.path("utenlandstilsnittBeskrivelse")
                        .asText(),
                beslutter = totrinnskontroll?.beslutterId,
                saksbehandler = totrinnskontroll?.saksbehandlerId,
                behandlingOpprettetAv = json.path("behandlingOpprettetAv").asText(),
                behandlingOpprettetType = json.path("behandlingOpprettetType").asText(),
                behandlingOpprettetTypeBeskrivelse = json.path("behandlingOpprettetTypeBeskrivelse")
                        .asText()
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

    private fun Behandling.resultatBegrunnelser(): List<ResultatBegrunnelseDVH> {
        val f = vedtakService.hentAktivForBehandling(behandlingId = id)?.vedtakBegrunnelser


        return when (resultat) {
            BehandlingResultat.HENLAGT_SØKNAD_TRUKKET, BehandlingResultat.HENLAGT_FEILAKTIG_OPPRETTET -> emptyList()
            else -> vedtakService.hentAktivForBehandling(behandlingId = id)?.vedtakBegrunnelser
                            ?.filter { it.begrunnelse != null }
                            ?.map {
                                ResultatBegrunnelseDVH(
                                        fom = it.fom,
                                        tom = it.tom,
                                        type = it.begrunnelse.vedtakBegrunnelseType.name,
                                        vedtakBegrunnelse = it.begrunnelse.name
                                )
                            } ?: emptyList()
        }
    }


    companion object {

        fun nyesteKontraktversjon(): String {
            return Utils.hentPropertyFraMaven("familie.kontrakter.saksstatistikk")
                ?: error("Fant ikke nyeste versjonsnummer for kontrakt")
        }
    }

}