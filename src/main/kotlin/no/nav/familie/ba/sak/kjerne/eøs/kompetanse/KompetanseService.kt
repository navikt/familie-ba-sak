package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaService
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSkjemaer
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilpassTil
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.hentBarnasEøsRegelverkTidslinjer
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.TidslinjeService
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompetanseService(
    private val tidslinjeService: TidslinjeService,
    kompetanseRepository: KompetanseRepository,
    tilbakestillBehandlingService: TilbakestillBehandlingService,
) {
    val serviceDelegate = PeriodeOgBarnSkjemaService(
        kompetanseRepository,
        tilbakestillBehandlingService
    )

    fun hentKompetanser(behandlingId: Long) =
        serviceDelegate.hentMedBehandlingId(behandlingId)

    fun hentKompetanse(kompetanseId: Long) =
        serviceDelegate.hentMedId(kompetanseId)

    @Transactional
    fun endreKompetanse(behandlingId: Long, oppdatering: Kompetanse) =
        serviceDelegate.endreSkjemaer(behandlingId, oppdatering)

    @Transactional
    fun slettKompetanse(kompetanseId: Long) =
        serviceDelegate.slettSkjema(kompetanseId)

    @Transactional
    fun tilpassKompetanserTilRegelverk(behandlingId: Long) {
        val gjeldendeKompetanser = hentKompetanser(behandlingId)

        val barnasEøsRegelverkTidslinjer = tidslinjeService.hentBarnasEøsRegelverkTidslinjer(behandlingId)

        val oppdaterteSkjemaer = gjeldendeKompetanser.tilSeparateTidslinjerForBarna()
            .tilpassTil(barnasEøsRegelverkTidslinjer) { Kompetanse.NULL }
            .tilSkjemaer(behandlingId)

        serviceDelegate.lagreSkjemaDifferanse(gjeldendeKompetanser, oppdaterteSkjemaer)
    }
}
