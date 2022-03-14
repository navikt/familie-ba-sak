package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.TidslinjeMedEksterntInnhold
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.erEnDelAvTidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.erInnenforTidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.PeriodeRepository

typealias PersonIdentDto = String

class KompetanseTidslinje(
    kompetanser: List<Kompetanse>,
    private val barn: Aktør,
    periodeRepository: PeriodeRepository
) : TidslinjeMedEksterntInnhold<Kompetanse>(
    kompetanser,
    periodeRepository
) {
    val behandlingId = innhold.first().behandlingId
    override val tidslinjeId = "Kompetanse.$behandlingId.${barn.aktørId}"

    override val fraOgMed =
        kompetanser.map { it.fom.tilTidspunktEllerUendeligLengeSiden { it.tom!! } }.minOrNull()!!
    override val tilOgMed: Tidspunkt =
        kompetanser.map { it.tom.tilTidspunktEllerUendeligLengeSiden { it.fom!! } }.maxOrNull()!!

    override fun genererPerioder(tidsrom: Tidsrom) = innhold
        .map { it.tilPeriode() }
        // Streng tolkning; perioden må være ekte innenfor tidsrommet. Kan gi hull i tidslinjen
        .filter { it.erInnenforTidsrom(tidsrom) }
        // Vid tolkning; periodeen må touch'e tidsrommet. Her vil vi kunne få overlappende perioder
        .filter { it.erEnDelAvTidsrom(tidsrom) }

    override fun innholdTilString(kompetanse: Kompetanse?): String {
        return kompetanse?.id?.toString()!!
    }
}

fun Kompetanse.tilPeriode() = Periode(
    fom = this.fom.tilTidspunktEllerUendeligLengeTil { tom!! },
    tom = this.tom.tilTidspunktEllerUendeligLengeTil { fom!! },
    innhold = this
)
