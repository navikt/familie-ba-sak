package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.eksterne.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.ResultatBegrunnelseDVH
import no.nav.familie.eksterne.kontrakter.saksstatistikk.SakDVH
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZonedDateTime

@Service
class SaksstatistikkConverter(
    private val vedtakService: VedtakService,
    private val totrinnskontrollService: TotrinnskontrollService,
) {

    fun konverterSakTilSisteKontraktVersjon(json: String): SakDVH {
        val sakDvhFraMellomlagring = sakstatistikkObjectMapper.readValue(json, SakDVH::class.java)
        return sakDvhFraMellomlagring.copy(versjon = nyesteKontraktversjon(), ytelseType = "BARNETRYGD")
    }

    fun konverterBehandlingTilSisteKontraktversjon(
        saksstatistikkMellomlagring: SaksstatistikkMellomlagring,
        behandling: Behandling,
    ): BehandlingDVH {
        when (saksstatistikkMellomlagring.kontraktVersjon) {
            "2.0_20201217113247_876a253" -> return konverterBehandlingMedKontraktVersjon_2_0_20201217113247_876a253(
                saksstatistikkMellomlagring,
                behandling
            )
            "2.0_20210128104331_9bd0bd2" -> return konverterBehandlingMedKontraktVersjon_2_0_20210128104331_9bd0bd2(
                saksstatistikkMellomlagring,
                behandling
            )

            //2.0_20210427132344_d9066f5 - kun en utvidelse av json-schema for vedttaksbegrunnelse
            //2.0_20210211132003_f81111d innfører ny struktur på resultbegrunnelser

            else -> {
                val totrinnskontroll =
                    totrinnskontrollService.hentAktivForBehandling(behandling.id)

                return saksstatistikkMellomlagring.jsonToBehandlingDVH()
                    .copy(versjon=  nyesteKontraktversjon(), saksbehandler = totrinnskontroll?.saksbehandlerId, beslutter = totrinnskontroll?.beslutterId)
            }
        }
    }


    /**
     * Flytter litt data rundt etter review med dvh. Legger til saksstatistikk
     */
    private fun konverterBehandlingMedKontraktVersjon_2_0_20201217113247_876a253(
        saksstatistikkMellomlagring: SaksstatistikkMellomlagring,
        behandling: Behandling,
    ): BehandlingDVH {
        val behandlingDvhFraMellomlagring: JsonNode =
            SaksstatistikkConverterService.sakstatistikkObjectMapper.readTree(saksstatistikkMellomlagring.json)
        val totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(behandling.id)
        return BehandlingDVH(
            funksjonellTid = behandlingDvhFraMellomlagring.path("funksjonellTid").asZonedDateTime(),
            tekniskTid = behandlingDvhFraMellomlagring.path("tekniskTid").asZonedDateTime(),
            mottattDato = behandlingDvhFraMellomlagring.path("mottattDato").asZonedDateTime(),
            registrertDato = behandlingDvhFraMellomlagring.path("registrertDato").asZonedDateTime(),
            behandlingId = behandling.id.toString(),
            funksjonellId = behandlingDvhFraMellomlagring.path("funksjonellId").asText(),
            sakId = behandlingDvhFraMellomlagring.path("sakId").asText(),
            behandlingType = behandlingDvhFraMellomlagring.path("behandlingType").asText(),
            behandlingStatus = behandlingDvhFraMellomlagring.path("behandlingStatus").asText(),
            behandlingKategori = behandlingDvhFraMellomlagring.path("behandlingUnderkategori").asText(),
            behandlingUnderkategori = null,
            behandlingAarsak = behandling.opprettetÅrsak.name,
            automatiskBehandlet = behandling.skalBehandlesAutomatisk,
            utenlandstilsnitt = behandlingDvhFraMellomlagring.path("behandlingKategori").asText(),
            ansvarligEnhetKode = behandlingDvhFraMellomlagring.path("ansvarligEnhetKode").asText(),
            behandlendeEnhetKode = behandlingDvhFraMellomlagring.path("behandlendeEnhetKode").asText(),
            ansvarligEnhetType = behandlingDvhFraMellomlagring.path("ansvarligEnhetType").asText(),
            behandlendeEnhetType = behandlingDvhFraMellomlagring.path("behandlendeEnhetType").asText(),
            totrinnsbehandling = behandlingDvhFraMellomlagring.path("totrinnsbehandling").asBoolean(),
            avsender = behandlingDvhFraMellomlagring.path("avsender").asText(),
            versjon = nyesteKontraktversjon(),
            vedtaksDato = behandlingDvhFraMellomlagring.path("vedtaksDato").asLocalDate(),
            relatertBehandlingId = behandlingDvhFraMellomlagring.path("relatertBehandlingId").asText(),
            vedtakId = behandlingDvhFraMellomlagring.path("vedtakId").asText(),
            resultat = behandlingDvhFraMellomlagring.path("resultat").asText(),
            resultatBegrunnelser = behandling.resultatBegrunnelser(),
            behandlingTypeBeskrivelse = behandlingDvhFraMellomlagring.path("behandlingTypeBeskrivelse").asText(),
            behandlingStatusBeskrivelse = behandlingDvhFraMellomlagring.path("behandlingStatusBeskrivelse")
                .asText(),
            utenlandstilsnittBeskrivelse = behandlingDvhFraMellomlagring.path("utenlandstilsnittBeskrivelse")
                .asText(),
            beslutter = totrinnskontroll?.beslutterId,
            saksbehandler = totrinnskontroll?.saksbehandlerId,
            behandlingOpprettetAv = behandlingDvhFraMellomlagring.path("behandlingOpprettetAv").asText(),
            behandlingOpprettetType = behandlingDvhFraMellomlagring.path("behandlingOpprettetType").asText(),
            behandlingOpprettetTypeBeskrivelse = behandlingDvhFraMellomlagring.path("behandlingOpprettetTypeBeskrivelse")
                .asText()
        )

    }


    private fun konverterBehandlingMedKontraktVersjon_2_0_20210128104331_9bd0bd2(
        saksstatistikkMellomlagring: SaksstatistikkMellomlagring,
        behandling: Behandling
    ): BehandlingDVH {

        val behandlingDvhFraMellomlagring: JsonNode =
            SaksstatistikkConverterService.sakstatistikkObjectMapper.readTree(saksstatistikkMellomlagring.json)
        val totrinnskontroll =
            totrinnskontrollService.hentAktivForBehandling(behandling.id)

        return BehandlingDVH(
            funksjonellTid = behandlingDvhFraMellomlagring.path("funksjonellTid").asZonedDateTime(),
            tekniskTid = behandlingDvhFraMellomlagring.path("tekniskTid").asZonedDateTime(),
            mottattDato = behandlingDvhFraMellomlagring.path("mottattDato").asZonedDateTime(),
            registrertDato = behandlingDvhFraMellomlagring.path("registrertDato").asZonedDateTime(),
            behandlingId = behandling.id.toString(),
            funksjonellId = behandlingDvhFraMellomlagring.path("funksjonellId").asText(),
            sakId = behandlingDvhFraMellomlagring.path("sakId").asText(),
            behandlingType = behandlingDvhFraMellomlagring.path("behandlingType").asText(),
            behandlingStatus = behandlingDvhFraMellomlagring.path("behandlingStatus").asText(),
            behandlingKategori = behandlingDvhFraMellomlagring.path("behandlingKategori").asText(),
            behandlingUnderkategori = behandlingDvhFraMellomlagring.path("behandlingUnderkategori").asText(),
            behandlingAarsak = behandlingDvhFraMellomlagring.path("behandlingAarsak").asText(),
            automatiskBehandlet = behandlingDvhFraMellomlagring.path("automatiskBehandlet").asBoolean(),
            utenlandstilsnitt = behandlingDvhFraMellomlagring.path("utenlandstilsnitt").asText(),
            ansvarligEnhetKode = behandlingDvhFraMellomlagring.path("ansvarligEnhetKode").asText(),
            behandlendeEnhetKode = behandlingDvhFraMellomlagring.path("behandlendeEnhetKode").asText(),
            ansvarligEnhetType = behandlingDvhFraMellomlagring.path("ansvarligEnhetType").asText(),
            behandlendeEnhetType = behandlingDvhFraMellomlagring.path("behandlendeEnhetType").asText(),
            totrinnsbehandling = behandlingDvhFraMellomlagring.path("totrinnsbehandling").asBoolean(),
            avsender = behandlingDvhFraMellomlagring.path("avsender").asText(),
            versjon = nyesteKontraktversjon(),
            vedtaksDato = behandlingDvhFraMellomlagring.path("vedtaksDato").asLocalDate(),
            relatertBehandlingId = behandlingDvhFraMellomlagring.path("relatertBehandlingId").asText(),
            vedtakId = behandlingDvhFraMellomlagring.path("vedtakId").asText(),
            resultat = behandlingDvhFraMellomlagring.path("resultat").asText(),
            resultatBegrunnelser = behandling.resultatBegrunnelser(),
            behandlingTypeBeskrivelse = behandlingDvhFraMellomlagring.path("behandlingTypeBeskrivelse").asText(),
            behandlingStatusBeskrivelse = behandlingDvhFraMellomlagring.path("behandlingStatusBeskrivelse")
                .asText(),
            utenlandstilsnittBeskrivelse = behandlingDvhFraMellomlagring.path("utenlandstilsnittBeskrivelse")
                .asText(),
            beslutter = totrinnskontroll?.beslutterId,
            saksbehandler = totrinnskontroll?.saksbehandlerId,
            behandlingOpprettetAv = behandlingDvhFraMellomlagring.path("behandlingOpprettetAv").asText(),
            behandlingOpprettetType = behandlingDvhFraMellomlagring.path("behandlingOpprettetType").asText(),
            behandlingOpprettetTypeBeskrivelse = behandlingDvhFraMellomlagring.path("behandlingOpprettetTypeBeskrivelse").asText()
        )
    }

    private fun JsonNode.asZonedDateTime(): ZonedDateTime {
        return ZonedDateTime.parse(asText())
    }

    private fun JsonNode.asLocalDate(): LocalDate? {
        return if (asText("").isNotEmpty()) LocalDate.parse(asText()) else null
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

        val sakstatistikkObjectMapper: ObjectMapper = objectMapper.copy()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)

        fun nyesteKontraktversjon(): String {
            return Utils.hentPropertyFraMaven("familie.kontrakter.saksstatistikk")
                ?: error("Fant ikke nyeste versjonsnummer for kontrakt")
        }


    }
}