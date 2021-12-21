package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class SaksstatistikkEventListener(
    private val saksstatistikkService: SaksstatistikkService,
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
    private val behandlingService: BehandlingService
) : ApplicationListener<SaksstatistikkEvent> {

    override fun onApplicationEvent(event: SaksstatistikkEvent) {
        if (event.behandlingId != null) {
            val behandling = behandlingService.hent(event.behandlingId)
            if (!behandling.erManuellMigrering()) {
                saksstatistikkService.mapTilBehandlingDVH(event.behandlingId)?.also {
                    saksstatistikkMellomlagringRepository.save(
                        SaksstatistikkMellomlagring(
                            funksjonellId = it.funksjonellId,
                            kontraktVersjon = it.versjon,
                            json = sakstatistikkObjectMapper.writeValueAsString(it),
                            type = SaksstatistikkMellomlagringType.BEHANDLING,
                            typeId = event.behandlingId
                        )
                    )
                }
            }
        } else if (event.fagsakId != null) {
            val behandling = behandlingService.hentAktivForFagsak(event.fagsakId)
            val erManuellMigrering = behandling?.erManuellMigrering() ?: false
            if (!erManuellMigrering) {
                saksstatistikkService.mapTilSakDvh(event.fagsakId)?.also {
                    saksstatistikkMellomlagringRepository.save(
                        SaksstatistikkMellomlagring(
                            funksjonellId = it.funksjonellId,
                            kontraktVersjon = it.versjon,
                            json = sakstatistikkObjectMapper.writeValueAsString(it),
                            type = SaksstatistikkMellomlagringType.SAK,
                            typeId = event.fagsakId
                        )
                    )
                }
            }
        }
    }
}

val sakstatistikkObjectMapper: ObjectMapper = objectMapper.copy()
    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
    .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
