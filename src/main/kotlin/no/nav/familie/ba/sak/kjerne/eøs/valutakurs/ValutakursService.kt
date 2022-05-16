package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ValutakursService(
    repository: ValutakursRepository,
    tilbakestillBehandlingService: TilbakestillBehandlingService,
) {
    val serviceDelegate = PeriodeOgBarnSkjemaService(
        repository,
        tilbakestillBehandlingService
    )

    fun oppdaterValutakurs(behandlingId: Long, valutakurs: Valutakurs) =
        serviceDelegate.endreSkjemaer(behandlingId, valutakurs)

    fun slettValutakurs(valutakursId: Long) =
        serviceDelegate.slettSkjema(valutakursId)

    @Transactional
    fun kopierOgErstattValutakurser(fraBehandlingId: Long, tilBehandlingId: Long) =
        serviceDelegate.kopierOgErstattSkjemaer(fraBehandlingId, tilBehandlingId)
}
