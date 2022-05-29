package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEndringAbonnent
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassTil
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilpassUtenlandskePeriodebeløpTilKompetanserService(
    utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    endringsabonnenter: Collection<PeriodeOgBarnSkjemaEndringAbonnent<UtenlandskPeriodebeløp>>,
    private val kompetanseService: KompetanseService
) : PeriodeOgBarnSkjemaEndringAbonnent<Kompetanse> {
    val skjemaService = PeriodeOgBarnSkjemaService(
        utenlandskPeriodebeløpRepository,
        endringsabonnenter
    )

    @Transactional
    fun tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId: BehandlingId) {
        val gjeldendeKompetanser = kompetanseService.hentKompetanser(behandlingId)

        tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId, gjeldendeKompetanser)
    }

    @Transactional
    override fun skjemaerEndret(behandlingId: BehandlingId, endretTil: Collection<Kompetanse>) {
        tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId, endretTil)
    }

    private fun tilpassUtenlandskPeriodebeløpTilKompetanser(
        behandlingId: BehandlingId,
        gjeldendeKompetanser: Collection<Kompetanse>
    ) {
        val forrigeUtenlandskePeriodebeløp = skjemaService.hentMedBehandlingId(behandlingId)

        val oppdaterteUtenlandskPeriodebeløp = tilpassUtenlandskePeriodebeløpTilKompetanser(
            forrigeUtenlandskePeriodebeløp,
            gjeldendeKompetanser
        ).medBehandlingId(behandlingId)

        skjemaService.lagreSkjemaDifferanse(
            behandlingId,
            forrigeUtenlandskePeriodebeløp,
            oppdaterteUtenlandskPeriodebeløp
        )
    }
}

internal fun tilpassUtenlandskePeriodebeløpTilKompetanser(
    forrigeUtenlandskePeriodebeløp: Iterable<UtenlandskPeriodebeløp>,
    gjeldendeKompetanser: Iterable<Kompetanse>
): Collection<UtenlandskPeriodebeløp> {
    val barnasKompetanseTidslinjer = gjeldendeKompetanser
        .tilSeparateTidslinjerForBarna()
        .filtrerSekundærland()

    return forrigeUtenlandskePeriodebeløp.tilSeparateTidslinjerForBarna()
        .tilpassTil(barnasKompetanseTidslinjer) { upb, _ -> upb ?: UtenlandskPeriodebeløp.NULL }
        .tilSkjemaer()
}

fun Map<Aktør, Tidslinje<Kompetanse, Måned>>.filtrerSekundærland() =
    this.mapValues { (_, tidslinje) -> tidslinje.filtrer { it?.resultat == KompetanseResultat.NORGE_ER_SEKUNDÆRLAND } }
