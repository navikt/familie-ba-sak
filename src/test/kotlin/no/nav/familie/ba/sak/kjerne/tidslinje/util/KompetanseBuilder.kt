package no.nav.familie.ba.sak.kjerne.tidslinje.util

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.SnittTidslinje
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
            .map { it.innhold!!.copy(fom = it.fom.tilYearMonth(), tom = it.tom.tilYearMonth()) }
            .all { kompetanser.add(it) }

        return this
    }

    fun byggKompetanser(): Collection<Kompetanse> = kompetanser
}

class KompetanseTidslinje(
    val charTidslinje: Tidslinje<Char>,
    val behandlingId: Long,
    val barn: List<Person>
) : SnittTidslinje<Kompetanse>(charTidslinje) {
    override fun beregnSnitt(tidspunkt: Tidspunkt): Kompetanse? {
        val tegn = charTidslinje.hentUtsnitt(tidspunkt)
        val barnFnr = barn.map { it.aktør.aktivFødselsnummer() }.toSet()
        val kompetanseMal = Kompetanse(behandlingId = behandlingId, fom = null, tom = null, barn = barnFnr)
        return when (tegn) {
            '-' -> kompetanseMal
            'S' -> kompetanseMal.copy(status = KompetanseStatus.OK, sekundærland = "NORGE")
            'P' -> kompetanseMal.copy(status = KompetanseStatus.OK, primærland = "NORGE")
            else -> null
        }
    }
}
