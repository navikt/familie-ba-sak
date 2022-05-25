package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ValutakursService(
    valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs>,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService
) {
    val skjemaService = PeriodeOgBarnSkjemaService(valutakursRepository)

    fun hentValutakurser(behandlingId: Long) =
        skjemaService.hentMedBehandlingId(behandlingId)

    fun oppdaterValutakurs(behandlingId: Long, valutakurs: Valutakurs) =
        skjemaService.endreSkjemaer(behandlingId, valutakurs)

    fun slettValutakurs(valutakursId: Long) =
        skjemaService.slettSkjema(valutakursId)

    @Transactional
    fun tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId: Long) {
        val forrigeValutakurser = hentValutakurser(behandlingId)
        val gjeldendeUtenlandskePeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        val oppdaterteValutakurser = tilpassValutakurserTilUtenlandskePeriodebeløp(
            forrigeValutakurser,
            gjeldendeUtenlandskePeriodebeløp
        ).medBehandlingId(behandlingId)

        skjemaService.lagreSkjemaDifferanse(forrigeValutakurser, oppdaterteValutakurser)
    }
}
