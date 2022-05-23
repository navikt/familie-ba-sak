package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassTil
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
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
        val forrigeValutakurser = hentValutakurser(behandlingId)
        val gjeldendeUtenlandskePeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        val oppdaterteValutakurser = tilpassValutakurserTilUtenlandskePeriodebeløp(
            forrigeValutakurser,
            gjeldendeUtenlandskePeriodebeløp
        ).medBehandlingId(behandlingId)

        serviceDelegate.lagreSkjemaDifferanse(forrigeValutakurser, oppdaterteValutakurser)
    }
}

internal fun tilpassValutakurserTilUtenlandskePeriodebeløp(
    forrigeValutakurser: Collection<Valutakurs>,
    gjeldendeUtenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>
): Collection<Valutakurs> {
    val barnasUtenlandskePeriodebeløpTidslinjer = gjeldendeUtenlandskePeriodebeløp
        .tilSeparateTidslinjerForBarna()

    return forrigeValutakurser.tilSeparateTidslinjerForBarna()
        .tilpassTil(barnasUtenlandskePeriodebeløpTidslinjer) { valutakurs, utenlandskPeriodebeløp ->
            when {
                valutakurs == null || valutakurs.valutakode != utenlandskPeriodebeløp.valutakode ->
                    Valutakurs.NULL.copy(valutakode = utenlandskPeriodebeløp.valutakode)
                else -> valutakurs
            }
        }
        .tilSkjemaer()
}
