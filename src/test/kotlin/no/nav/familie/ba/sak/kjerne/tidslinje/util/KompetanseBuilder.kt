package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import java.time.YearMonth

class KompetanseBuilder(
    val behandling: Behandling = lagBehandling(),
    val startMåned: YearMonth = YearMonth.of(2020, 1)
) {
    val kompetanser: MutableList<Kompetanse> = mutableListOf()

    fun medKompetanse(k: String, vararg barn: Person): KompetanseBuilder {
        val charTidslinje = k.tilCharTidslinje(startMåned)
        val kompetanseTidslinje = KompetanseTidslinje(charTidslinje, behandling.id, barn.toList())

        kompetanseTidslinje.perioder()
            .filter { it.innhold != null }
            .map { it.innhold!!.copy(fom = it.fraOgMed.tilYearMonth(), tom = it.tilOgMed.tilYearMonth()) }
            .all { kompetanser.add(it) }

        return this
    }

    fun byggKompetanser(): Collection<Kompetanse> = kompetanser
}

class KompetanseTidslinje(
    val charTidslinje: Tidslinje<Char, YearMonth>,
    val behandlingId: Long,
    val barn: List<Person>
) : TidslinjeSomStykkerOppTiden<Kompetanse, YearMonth>(charTidslinje) {
    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<YearMonth>): Kompetanse? {
        val tegn = charTidslinje.hentUtsnitt(tidspunkt)
        val barnAktørIder = barn.map { it.aktør.aktørId }.toSet()
        val kompetanseMal =
            Kompetanse(behandlingId = behandlingId, fom = null, tom = null, barnAktørIder = barnAktørIder)
        return when (tegn) {
            '-' -> kompetanseMal
            'S' -> kompetanseMal.copy(status = KompetanseStatus.OK, sekundærland = "NORGE")
            'P' -> kompetanseMal.copy(status = KompetanseStatus.OK, primærland = "NORGE")
            else -> null
        }
    }
}
