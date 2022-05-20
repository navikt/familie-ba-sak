package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassTil
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ValutakursService(
    repository: PeriodeOgBarnSkjemaRepository<Valutakurs>,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService
) {
    val serviceDelegate = PeriodeOgBarnSkjemaService(repository)

    fun hentValutakurser(behandlingId: Long) =
        serviceDelegate.hentMedBehandlingId(behandlingId)

    fun oppdaterValutakurs(behandlingId: Long, valutakurs: Valutakurs) =
        serviceDelegate.endreSkjemaer(behandlingId, valutakurs)

    fun slettValutakurs(valutakursId: Long) =
        serviceDelegate.slettSkjema(valutakursId)

    @Transactional
    fun tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId: Long) {
        val gjeldendeValutakurser = hentValutakurser(behandlingId)

        val barnasUtenlandskePeriodebeløpTidslinjer = utenlandskPeriodebeløpService
            .hentUtenlandskePeriodebeløp(behandlingId)
            .tilSeparateTidslinjerForBarna()

        val oppdaterteValutakurser = gjeldendeValutakurser.tilSeparateTidslinjerForBarna()
            .tilpassTil(barnasUtenlandskePeriodebeløpTidslinjer) { Valutakurs.NULL }
            .tilSkjemaer(behandlingId)

        serviceDelegate.lagreSkjemaDifferanse(gjeldendeValutakurser, oppdaterteValutakurser)
    }
}
