package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.slåSammen
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.tilpassKompetanserTilRegelverk
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.beregning.trekkFra
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.inneholder
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.utenSkjema
import no.nav.familie.ba.sak.kjerne.eøs.tidslinjer.TidslinjeService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidsenhet
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tilOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KompetanseService(
    val tidslinjeService: TidslinjeService,
    val kompetanseRepository: KompetanseRepository,
    val tilbakestillBehandlingService: TilbakestillBehandlingService,
) {

    fun hentKompetanser(behandlingId: Long): Collection<Kompetanse> {
        return kompetanseRepository.findByBehandlingId(behandlingId)
    }

    fun hentKompetanse(kompetanseId: Long): Kompetanse {
        return kompetanseRepository.getById(kompetanseId)
    }

    @Transactional
    fun oppdaterKompetanse(kompetanseId: Long, oppdatertKompetanse: Kompetanse) {
        val kompetanseSomOppdateres = kompetanseRepository.getById(kompetanseId)

        if (!kompetanseSomOppdateres.utenSkjema().inneholder(oppdatertKompetanse.utenSkjema()))
            throw IllegalArgumentException("Endringen er ikke innenfor kompetansen som endres")

        val behandlingId = kompetanseSomOppdateres.behandlingId
        val gjeldendeKompetanser = hentKompetanser(behandlingId)

        val kompetanseFratrukketOppdatering = kompetanseSomOppdateres.trekkFra(oppdatertKompetanse)
        val oppdaterteKompetanser =
            gjeldendeKompetanser.plus(kompetanseFratrukketOppdatering)
                .plus(oppdatertKompetanse).minus(kompetanseSomOppdateres)
                .slåSammen().medBehandlingId(behandlingId)

        lagreKompetanseDifferanse(gjeldendeKompetanser, oppdaterteKompetanser)
        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
    }

    @Transactional
    fun slettKompetanse(kompetanseId: Long) {
        val kompetanseTilSletting = kompetanseRepository.getById(kompetanseId)
        val behandlingId = kompetanseTilSletting.behandlingId
        val gjeldendeKompetanser = hentKompetanser(behandlingId)
        val blankKompetanse = kompetanseTilSletting.utenSkjema()

        val oppdaterteKompetanser = gjeldendeKompetanser.minus(kompetanseTilSletting).plus(blankKompetanse)
            .slåSammen().medBehandlingId(behandlingId)

        lagreKompetanseDifferanse(gjeldendeKompetanser, oppdaterteKompetanser)

        tilbakestillBehandlingService.tilbakestillBehandlingTilBehandlingsresultat(behandlingId)
    }

    @Transactional
    fun tilpassKompetanserTilRegelverk(behandlingId: Long) {
        val gjeldendeKompetanser = hentKompetanser(behandlingId)
        val barnasRegelverkTidslinjer = tidslinjeService.hentBarnasRegelverkTidslinjer(behandlingId)

        val oppdaterteKompetanser = tilpassKompetanserTilRegelverk(gjeldendeKompetanser, barnasRegelverkTidslinjer)
            .medBehandlingId(behandlingId)

        lagreKompetanseDifferanse(gjeldendeKompetanser, oppdaterteKompetanser)
    }

    private fun lagreKompetanseDifferanse(gjeldende: Collection<Kompetanse>, oppdaterte: Collection<Kompetanse>) {
        kompetanseRepository.deleteAll(gjeldende - oppdaterte)
        kompetanseRepository.saveAll(oppdaterte - gjeldende)
    }

    private fun Collection<Kompetanse>.medBehandlingId(behandlingId: Long): Collection<Kompetanse> {
        this.forEach { it.behandlingId = behandlingId }
        return this
    }

    private fun TidslinjeService.hentBarnasRegelverkTidslinjer(behandlingId: Long): Map<Aktør, Tidslinje<Regelverk, Måned>> =
        this.hentTidslinjerThrows(behandlingId).barnasTidslinjer()
            .mapValues { (_, tidslinjer) ->
                tidslinjer.regelverkTidslinje
                    .map { it?.regelverk }
                    .filtrerIkkeNull()
                    .forlengFremtidTilUendelig(MånedTidspunkt.nå())
            }
            .mapKeys { (aktør, _) -> aktør }
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.forlengFremtidTilUendelig(nå: Tidspunkt<T>): Tidslinje<I, T> {
    return if (this.tilOgMed() > nå)
        this.flyttTilOgMed(this.tilOgMed().somUendeligLengeTil())
    else
        this
}

fun <I, T : Tidsenhet> Tidslinje<I, T>.flyttTilOgMed(tilTidspunkt: Tidspunkt<T>): Tidslinje<I, T> {
    val tidslinje = this

    return if (tilTidspunkt < tidslinje.fraOgMed())
        TomTidslinje()
    else
        object : Tidslinje<I, T>() {
            override fun lagPerioder(): Collection<Periode<I, T>> = tidslinje.perioder()
                .filter { it.fraOgMed <= tilTidspunkt }
                .replaceLast { Periode(it.fraOgMed, tilTidspunkt, it.innhold) }
        }
}

fun <T> Collection<T>.replaceLast(replacer: (T) -> T) =
    this.take(this.size - 1) + replacer(this.last())
