package no.nav.familie.ba.sak.kjerne.tidslinje.eksempler

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.TidslinjeUtenAvhengigheter
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidspunkt
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Tidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.erEnDelAvTidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.erInnenforTidsrom
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeTil
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.PeriodeRepository
import no.nav.familie.ba.sak.kjerne.tidslinje.TidslinjeRepository
import no.nav.familie.ba.sak.kjerne.tidslinje.tilDto
import no.nav.familie.ba.sak.kjerne.tidslinje.tilPeriode

typealias PersonIdentDto = String

class KompetanseTidslinje(
    private val kompetanser: List<Kompetanse>,
    private val barn: Aktør
) : TidslinjeUtenAvhengigheter<Kompetanse>(
    KompetanseTidslinjeRepository(
        barn,
        kompetanser,
        MockPerideRepository()
    )
) {
    override val fraOgMed =
        kompetanser.map { it.fom.tilTidspunktEllerUendeligLengeSiden { it.tom!! } }.minOrNull()!!
    override val tilOgMed: Tidspunkt =
        kompetanser.map { it.tom.tilTidspunktEllerUendeligLengeSiden { it.fom!! } }.maxOrNull()!!

    override fun genererPerioder(tidsrom: Tidsrom) = kompetanser
        .map { it.tilPeriode() }
        // Streng tolkning; perioden må være ekte innenfor tidsrommet. Kan gi hull i tidslinjen
        .filter { it.erInnenforTidsrom(tidsrom) }
        // Vid tolkning; periodeen må touch'e tidsrommet. Her vil vi kunne få overlappende perioder
        .filter { it.erEnDelAvTidsrom(tidsrom) }
}

class KompetanseTidslinjeRepository(
    private val aktør: Aktør,
    private val kompetanser: Collection<Kompetanse>,
    private val periodeRepository: PeriodeRepository
) : TidslinjeRepository<Kompetanse> {

    val behandlingId = kompetanser.first().behandlingId
    val tidslinjeId = "Kompetanse.$behandlingId.${aktør.aktørId}"

    override fun lagre(perioder: Collection<Periode<Kompetanse>>): Collection<Periode<Kompetanse>> {
        return periodeRepository
            .lagrePerioder(tidslinjeId, perioder.map { it.tilDto(it.id.toString()) })
            .map { dto ->
                dto.tilPeriode { ref -> kompetanser.find { it.id.toString() == ref } }
            }
    }

    override fun hent(): Collection<Periode<Kompetanse>> {

        return periodeRepository.hentPerioder(
            tidslinjeId = tidslinjeId,
            innholdReferanser = kompetanser.map { it.id.toString() }
        ).map {
            it.tilPeriode { ref -> kompetanser.find { it.id.toString() == ref } }
        }
    }
}

fun Kompetanse.tilPeriode() = Periode(
    fom = this.fom.tilTidspunktEllerUendeligLengeTil { tom!! },
    tom = this.tom.tilTidspunktEllerUendeligLengeTil { fom!! },
    innhold = this
)
