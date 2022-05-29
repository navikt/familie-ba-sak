package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
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
class TilpassValutakurserTilUtenlandskePeriodebeløpService(
    valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs>,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<Valutakurs>>
) : PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp> {
    val skjemaService = PeriodeOgBarnSkjemaService(
        valutakursRepository,
        endringsabonnenter
    )

    @Transactional
    fun tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId: BehandlingId) {
        val gjeldendeUtenlandskePeriodebeløp = utenlandskPeriodebeløpService.hentUtenlandskePeriodebeløp(behandlingId)

        tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId, gjeldendeUtenlandskePeriodebeløp)
    }

    @Transactional
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        endretTil: Collection<UtenlandskPeriodebeløp>
    ) {
        tilpassValutakursTilUtenlandskPeriodebeløp(behandlingId, endretTil)
    }

    private fun tilpassValutakursTilUtenlandskPeriodebeløp(
        behandlingId: BehandlingId,
        gjeldendeUtenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>
    ) {
        val forrigeValutakurser = skjemaService.hentMedBehandlingId(behandlingId)

        val oppdaterteValutakurser = tilpassValutakurserTilUtenlandskePeriodebeløp(
            forrigeValutakurser,
            gjeldendeUtenlandskePeriodebeløp
        ).medBehandlingId(behandlingId)

        skjemaService.lagreSkjemaDifferanse(behandlingId, forrigeValutakurser, oppdaterteValutakurser)
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
