package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedClient
import no.nav.familie.ba.sak.infotrygd.InfotrygdVedtakFeedDto
import org.springframework.stereotype.Service

@Service
class SendVedtakFeedTilInfotrygd(
        private val vedtakService: VedtakService,
        private val infotrygdFeedClient: InfotrygdFeedClient) : BehandlingSteg<SendVedtakFeedTilInfotrygdDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: SendVedtakFeedTilInfotrygdDTO): StegType {

        val vedtak = vedtakService.hent(vedtakId = data.vedtakId)
        val status = behandling.status

        if(!(status == BehandlingStatus.IVERKSATT || status == BehandlingStatus.FERDIGSTILT) || vedtak.vedtaksdato == null) {
            throw Exception("Behandling kan ikke avsluttes i Infotrygd om den ikke er iverksatt eller ferdigstilt.")
        }

        val fnrStonadsmottaker = vedtak.behandling.fagsak.hentAktivIdent().ident
        val infotrygdVedtakFeedDto = InfotrygdVedtakFeedDto(fnrStonadsmottaker, vedtak.vedtaksdato!!)

        infotrygdFeedClient.sendVetakFeedTilInfotrygd(infotrygdVedtakFeedDto)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.SEND_VETAKS_FEED_TIL_INFOTRYGD
    }
}

data class SendVedtakFeedTilInfotrygdDTO(val vedtakId: Long)
