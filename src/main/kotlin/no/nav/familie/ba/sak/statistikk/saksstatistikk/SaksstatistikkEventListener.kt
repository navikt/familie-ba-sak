package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import no.nav.familie.kontrakter.felles.jsonMapperBuilder
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class SaksstatistikkEventListener(
    private val saksstatistikkService: SaksstatistikkService,
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
) : ApplicationListener<SaksstatistikkEvent> {
    override fun onApplicationEvent(event: SaksstatistikkEvent) {
        if (event.behandlingId != null) {
            saksstatistikkService.mapTilBehandlingDVH(event.behandlingId)?.also {
                saksstatistikkMellomlagringRepository.save(
                    SaksstatistikkMellomlagring(
                        funksjonellId = it.funksjonellId,
                        kontraktVersjon = it.versjon,
                        json = sakstatistikkObjectMapper.writeValueAsString(it),
                        type = SaksstatistikkMellomlagringType.BEHANDLING,
                        typeId = event.behandlingId,
                    ),
                )
            }
        } else if (event.fagsakId != null) {
            saksstatistikkService.mapTilSakDvh(event.fagsakId)?.also {
                saksstatistikkMellomlagringRepository.save(
                    SaksstatistikkMellomlagring(
                        funksjonellId = it.funksjonellId,
                        kontraktVersjon = it.versjon,
                        json = sakstatistikkObjectMapper.writeValueAsString(it),
                        type = SaksstatistikkMellomlagringType.SAK,
                        typeId = event.fagsakId,
                    ),
                )
            }
        }
    }
}

val sakstatistikkObjectMapper: ObjectMapper =
    jsonMapperBuilder
        .changeDefaultPropertyInclusion {
            JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY)
        }
//        .defaultTimeZone(TimeZone.getTimeZone("Europe/Oslo")) // TODO test
        .build()
