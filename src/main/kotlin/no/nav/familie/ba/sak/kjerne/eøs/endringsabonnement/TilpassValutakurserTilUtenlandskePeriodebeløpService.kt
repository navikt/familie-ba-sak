package no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.FinnPeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.outerJoin
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class TilpassValutakurserTilUtenlandskePeriodebeløpService(
    valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs>,
    private val utenlandskPeriodebeløpRepository: FinnPeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<Valutakurs>>,
    private val clockProvider: ClockProvider,
) : PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    val skjemaService =
        PeriodeOgBarnSkjemaService(
            valutakursRepository,
            endringsabonnenter,
        )

    @Transactional
    fun tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId: BehandlingId) {
        val gjeldendeUtenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId.id)

        tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId, gjeldendeUtenlandskePeriodebeløp)
    }

    @Transactional
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        endretTil: Collection<UtenlandskPeriodebeløp>,
    ) {
        tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId, endretTil)
    }

    private fun tilpassValutakursTilUtenlandskPeriodebeløp(
        behandlingId: BehandlingId,
        gjeldendeUtenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    ) {
        val forrigeValutakurser = skjemaService.hentMedBehandlingId(behandlingId)

        val oppdaterteValutakurser =
            tilpassValutakurserTilUtenlandskePeriodebeløp(
                forrigeValutakurser,
                gjeldendeUtenlandskePeriodebeløp,
                clockProvider,
            ).medBehandlingId(behandlingId)

        skjemaService.lagreDifferanseOgVarsleAbonnenter(behandlingId, forrigeValutakurser, oppdaterteValutakurser)
    }
}

internal fun tilpassValutakurserTilUtenlandskePeriodebeløp(
    forrigeValutakurser: Collection<Valutakurs>,
    gjeldendeUtenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    clockProvider: ClockProvider,
): Collection<Valutakurs> {
    val barnasUtenlandskePeriodebeløpTidslinjer =
        gjeldendeUtenlandskePeriodebeløp
            .tilSeparateTidslinjerForBarna()

    val korrigerteValutakurser = korrigerValutakurserMedFremdtidigTom(forrigeValutakurser, clockProvider)
    return korrigerteValutakurser
        .tilSeparateTidslinjerForBarna()
        .outerJoin(barnasUtenlandskePeriodebeløpTidslinjer) { valutakurs, utenlandskPeriodebeløp ->
            when {
                utenlandskPeriodebeløp == null -> null
                valutakurs == null || valutakurs.valutakode != utenlandskPeriodebeløp.valutakode ->
                    Valutakurs.NULL.copy(valutakode = utenlandskPeriodebeløp.valutakode)
                else -> valutakurs
            }
        }.tilSkjemaer()
}

private fun korrigerValutakurserMedFremdtidigTom(
    gjeldendeValutakurser: Iterable<Valutakurs>,
    clockProvider: ClockProvider,
): Iterable<Valutakurs> =
    gjeldendeValutakurser
        .mapNotNull {
            val inneværendeMåned = YearMonth.now(clockProvider.get())
            when {
                it.fom != null && it.fom.isAfter(inneværendeMåned) -> null
                it.tom != null && it.tom.isSameOrAfter(inneværendeMåned) -> it.copy(tom = null)
                else -> it
            }
        }
