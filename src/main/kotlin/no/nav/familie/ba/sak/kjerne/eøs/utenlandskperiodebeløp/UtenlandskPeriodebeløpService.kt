package no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp

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
class UtenlandskPeriodebeløpService(
    repository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>,
    private val kompetanseService: KompetanseService
) {
    val serviceDelegate = PeriodeOgBarnSkjemaService(repository)

    fun hentUtenlandskePeriodebeløp(behandlingId: Long) =
        serviceDelegate.hentMedBehandlingId(behandlingId)

    fun oppdaterUtenlandskPeriodebeløp(behandlingId: Long, utenlandskPeriodebeløp: UtenlandskPeriodebeløp) =
        serviceDelegate.endreSkjemaer(behandlingId, utenlandskPeriodebeløp)

    fun slettUtenlandskPeriodebeløp(utenlandskPeriodebeløpId: Long) =
        serviceDelegate.slettSkjema(utenlandskPeriodebeløpId)

    @Transactional
    fun tilpassUtenlandskPeriodebeløpTilKompetanser(behandlingId: Long) {
        val forrigeUtenlandskePeriodebeløp = hentUtenlandskePeriodebeløp(behandlingId)
        val gjeldendeKompetanser = kompetanseService.hentKompetanser(behandlingId)

        val oppdaterteUtenlandskPeriodebeløp = tilpassUtenlandskePeriodebeløpTilKompetanser(
            forrigeUtenlandskePeriodebeløp,
            gjeldendeKompetanser
        ).medBehandlingId(behandlingId)

        serviceDelegate.lagreSkjemaDifferanse(forrigeUtenlandskePeriodebeløp, oppdaterteUtenlandskPeriodebeløp)
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
