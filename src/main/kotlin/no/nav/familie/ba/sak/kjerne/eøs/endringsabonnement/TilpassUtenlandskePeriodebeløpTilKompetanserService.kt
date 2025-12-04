package no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.FinnPeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.erNorgeSekundærLand
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utbetalingsland
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.forlengFremtidTilUendelig
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.filtrer
import no.nav.familie.tidslinje.utvidelser.outerJoin
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class TilpassUtenlandskePeriodebeløpTilKompetanserService(
    utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp>>,
    private val kompetanseRepository: FinnPeriodeOgBarnSkjemaRepository<Kompetanse>,
    private val clockProvider: ClockProvider,
) : PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse> {
    val skjemaService =
        PeriodeOgBarnSkjemaService(
            utenlandskPeriodebeløpRepository,
            endringsabonnenter,
        )

    @Transactional
    fun tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId: BehandlingId) {
        val gjeldendeKompetanser = kompetanseRepository.finnFraBehandlingId(behandlingId.id)

        tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId, gjeldendeKompetanser)
    }

    @Transactional
    override fun skjemaerEndret(
        behandlingId: BehandlingId,
        endretTil: Collection<Kompetanse>,
    ) {
        tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId, endretTil)
    }

    private fun tilpassUtenlandskPeriodebeløpTilKompetanser(
        behandlingId: BehandlingId,
        gjeldendeKompetanser: Collection<Kompetanse>,
    ) {
        val forrigeUtenlandskePeriodebeløp = skjemaService.hentMedBehandlingId(behandlingId)

        val oppdaterteUtenlandskPeriodebeløp =
            tilpassUtenlandskePeriodebeløpTilKompetanser(
                forrigeUtenlandskePeriodebeløp = forrigeUtenlandskePeriodebeløp,
                gjeldendeKompetanser = gjeldendeKompetanser,
                inneværendeMåned = YearMonth.now(clockProvider.get()),
            ).medBehandlingId(behandlingId)

        skjemaService.lagreDifferanseOgVarsleAbonnenter(
            behandlingId,
            forrigeUtenlandskePeriodebeløp,
            oppdaterteUtenlandskPeriodebeløp,
        )
    }
}

internal fun tilpassUtenlandskePeriodebeløpTilKompetanser(
    forrigeUtenlandskePeriodebeløp: Iterable<UtenlandskPeriodebeløp>,
    gjeldendeKompetanser: Iterable<Kompetanse>,
    inneværendeMåned: YearMonth,
): Collection<UtenlandskPeriodebeløp> {
    val barnasKompetanseTidslinjer =
        gjeldendeKompetanser
            .tilSeparateTidslinjerForBarna()
            .filtrerSekundærland()

    return forrigeUtenlandskePeriodebeløp
        .tilSeparateTidslinjerForBarna()
        .outerJoin(barnasKompetanseTidslinjer) { upb, kompetanse ->
            val utbetalingsland = kompetanse?.utbetalingsland()
            when {
                kompetanse == null -> {
                    null
                }

                upb == null || upb.utbetalingsland != utbetalingsland -> {
                    UtenlandskPeriodebeløp.NULL.copy(utbetalingsland = utbetalingsland)
                }

                else -> {
                    upb
                }
            }
        }.mapValues { (_, tidslinje) ->
            tidslinje.forlengFremtidTilUendelig(tidspunktForUendelighet = inneværendeMåned.sisteDagIInneværendeMåned())
        }.tilSkjemaer()
}

private fun Map<Aktør, Tidslinje<Kompetanse>>.filtrerSekundærland() =
    this.mapValues { (_, tidslinje) ->
        tidslinje.filtrer { it?.erNorgeSekundærLand() == true }
    }
