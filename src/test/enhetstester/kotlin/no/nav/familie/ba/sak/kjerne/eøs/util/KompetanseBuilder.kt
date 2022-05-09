package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.innholdForTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt

class KompetanseBuilder(
    val startMåned: Tidspunkt<Måned> = jan(2020),
    val behandlingId: Long = 1
) {
    val kompetanser: MutableList<Kompetanse> = mutableListOf()

    fun medKompetanse(k: String, vararg barn: Person): KompetanseBuilder {
        val charTidslinje = k.tilCharTidslinje(startMåned)
        val kompetanseTidslinje = KompetanseTidslinje(charTidslinje, barn.toList())

        kompetanseTidslinje.perioder()
            .filter { it.innhold != null }
            .map {
                it.innhold!!.copy(
                    fom = it.fraOgMed.tilYearMonthEllerNull(),
                    tom = it.tilOgMed.tilYearMonthEllerNull()
                )
            }
            .all { kompetanser.add(it) }

        return this
    }

    fun byggKompetanser(): Collection<Kompetanse> = kompetanser
        .map { kompetanse -> kompetanse.also { it.behandlingId = behandlingId } }
}

internal class KompetanseTidslinje(
    val charTidslinje: Tidslinje<Char, Måned>,
    val barn: List<Person>
) : TidslinjeSomStykkerOppTiden<Kompetanse, Måned>(charTidslinje) {
    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<Måned>): Kompetanse? {
        val tegn = charTidslinje.innholdForTidspunkt(tidspunkt)
        val barnAktørIder = barn.map { it.aktør }.toSet()
        val kompetanseMal =
            Kompetanse(fom = null, tom = null, barnAktører = barnAktørIder)
        return when (tegn) {
            '-' -> kompetanseMal
            'S' -> kompetanseMal.copy(resultat = KompetanseResultat.NORGE_ER_SEKUNDÆRLAND)
            'P' -> kompetanseMal.copy(resultat = KompetanseResultat.NORGE_ER_PRIMÆRLAND)
            'T' -> kompetanseMal.copy(resultat = KompetanseResultat.TO_PRIMÆRLAND)
            else -> null
        }
    }
}
