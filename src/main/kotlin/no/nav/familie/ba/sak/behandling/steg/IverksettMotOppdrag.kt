package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.task.dto.IverksettingTaskDTO
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import org.springframework.stereotype.Service

@Service
class IverksettMotOppdrag(private val økonomiService: ØkonomiService) : BehandlingSteg<IverksettingTaskDTO> {

    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: IverksettingTaskDTO,
                                      stegService: StegService?): StegType {
        val vilkårsvurdering: Vilkårsvurdering = stegService?.hentBehandlingSteg(StegType.VILKÅRSVURDERING) as Vilkårsvurdering

        vilkårsvurdering.validerSteg(behandling)
        økonomiService.oppdaterTilkjentYtelseOgIverksettVedtak(data.behandlingsId,
                                                               data.vedtaksId,
                                                               data.saksbehandlerId)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.IVERKSETT_MOT_OPPDRAG
    }
}