package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.tilAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MAX_MÅNED
import no.nav.familie.ba.sak.kjerne.eøs.felles.util.MIN_MÅNED
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrer
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerUtenNullMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerSenereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt.Companion.tilTidspunktEllerTidligereEnn
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import java.math.BigDecimal
import java.time.LocalDate

class TilkjentYtelseBuilder(
    private val startMåned: Tidspunkt<Måned>,
    private val behandling: Behandling = lagBehandling()
) {
    private val tilkjentYtelse = TilkjentYtelse(
        behandling = behandling,
        opprettetDato = LocalDate.now(),
        endretDato = LocalDate.now()
    )

    var gjeldendePersoner: List<Person> = emptyList()

    fun forPersoner(vararg personer: Person): TilkjentYtelseBuilder {
        gjeldendePersoner = personer.toList()
        return this
    }

    fun medOrdinær(
        s: String,
        prosent: Long = 100,
        nasjonalt: (Int) -> Int? = { null },
        differanse: (Int) -> Int? = { null },
        kalkulert: (Int) -> Int = { it }
    ): TilkjentYtelseBuilder {

        val andeler = gjeldendePersoner
            .map { person ->
                val andelTilkjentYtelseTidslinje = s.tilCharTidslinje(startMåned)
                    .filtrer { char -> char?.let { !it.isWhitespace() } ?: false }
                    .map {
                        AndelTilkjentYtelse(
                            behandlingId = behandling.id,
                            tilkjentYtelse = tilkjentYtelse,
                            aktør = person.aktør,
                            stønadFom = MIN_MÅNED,
                            stønadTom = MAX_MÅNED,
                            kalkulertUtbetalingsbeløp = 0, // Overskrives under
                            nasjonaltPeriodebeløp = 0, // Overskrives under
                            differanseberegnetPeriodebeløp = null, // Overskrives under
                            prosent = BigDecimal.valueOf(prosent),
                            sats = 0, // Overskrives under
                            type = YtelseType.ORDINÆR_BARNETRYGD
                        )
                    }

                val orbaTidslinje = satstypeTidslinje(SatsType.ORBA)
                val tilleggOrbaTidslinje = satstypeTidslinje(SatsType.TILLEGG_ORBA)
                    .filtrerMed(under6ÅrTidslinje(person))
                val satsTidslinje = orbaTidslinje.kombinerMed(tilleggOrbaTidslinje) { orba, tillegg -> tillegg ?: orba }

                andelTilkjentYtelseTidslinje.kombinerUtenNullMed(satsTidslinje) { aty, sats ->
                    aty.copy(
                        sats = sats,
                        kalkulertUtbetalingsbeløp = kalkulert(sats),
                        nasjonaltPeriodebeløp = nasjonalt(sats) ?: kalkulert(sats),
                        differanseberegnetPeriodebeløp = differanse(sats)
                    )
                }
            }.tilAndelerTilkjentYtelse()

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andeler)
        return this
    }

    fun bygg(): TilkjentYtelse = tilkjentYtelse
}

private fun satstypeTidslinje(satsType: SatsType) = tidslinje {
    SatsService.hentAllesatser()
        .filter { it.type == satsType }
        .map {
            val fom = if (it.gyldigFom == LocalDate.MIN) null else it.gyldigFom.toYearMonth()
            val tom = if (it.gyldigTom == LocalDate.MAX) null else it.gyldigTom.toYearMonth()
            Periode(
                fraOgMed = fom.tilTidspunktEllerTidligereEnn(tom),
                tilOgMed = tom.tilTidspunktEllerSenereEnn(fom),
                it.beløp
            )
        }
}

private fun under6ÅrTidslinje(person: Person) = tidslinje {
    listOf(
        Periode(
            person.fødselsdato.toYearMonth().tilTidspunkt(),
            person.fødselsdato.toYearMonth().plusYears(6).minusMonths(1).tilTidspunkt(),
            true
        )
    )
}
