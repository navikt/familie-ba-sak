package no.nav.familie.ba.sak.statistikk.saksstatistikk

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagring
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

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
    JsonMapper
        .builder()
        .addModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .changeDefaultPropertyInclusion {
            JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.NON_EMPTY)
        }.build()
