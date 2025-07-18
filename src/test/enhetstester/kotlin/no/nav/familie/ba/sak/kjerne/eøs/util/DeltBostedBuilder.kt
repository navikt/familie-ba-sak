package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagEndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.AndelTilkjentYtelseMedEndretUtbetalingGenerator
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import java.time.YearMonth

class DeltBostedBuilder(
    startMåned: YearMonth = jan(2020),
    internal val tilkjentYtelse: TilkjentYtelse,
) : SkjemaBuilder<DeltBosted, DeltBostedBuilder>(startMåned, BehandlingId(tilkjentYtelse.behandling.id)) {
    fun medDeltBosted(
        k: String,
        vararg barn: Person,
    ) = medSkjema(k, barn.toList()) {
        when (it) {
            '0' -> DeltBosted(prosent = 0, barnPersoner = barn.toList())
            '/' -> DeltBosted(prosent = 50, barnPersoner = barn.toList())
            '1' -> DeltBosted(prosent = 100, barnPersoner = barn.toList())
            else -> null
        }
    }
}

data class DeltBosted(
    override val fom: YearMonth? = null,
    override val tom: YearMonth? = null,
    override val barnAktører: Set<Aktør> = emptySet(),
    val prosent: Int?,
    internal val barnPersoner: List<Person> = emptyList(),
) : PeriodeOgBarnSkjemaEntitet<DeltBosted>() {
    override fun utenInnhold() = copy(prosent = null)

    override fun kopier(
        fom: YearMonth?,
        tom: YearMonth?,
        barnAktører: Set<Aktør>,
    ) = copy(
        fom = fom,
        tom = tom,
        barnAktører = barnAktører.map { it.copy() }.toSet(),
        barnPersoner = this.barnPersoner.filter { barnAktører.contains(it.aktør) },
    ).also {
        if (barnAktører.size != barnPersoner.size) {
            throw Feil("Ikke samsvar mellom antall aktører og barn lenger")
        }
    }

    override var id: Long = 0
    override var behandlingId: Long = 0
}

fun DeltBostedBuilder.oppdaterTilkjentYtelse(): TilkjentYtelse {
    val andelerTilkjentYtelserEtterEUA =
        AndelTilkjentYtelseMedEndretUtbetalingGenerator.lagAndelerMedEndretUtbetalingAndeler(
            andelTilkjentYtelserUtenEndringer = tilkjentYtelse.andelerTilkjentYtelse.toList(),
            endretUtbetalingAndeler = bygg().tilEndreteUtebetalingAndeler(),
            tilkjentYtelse = tilkjentYtelse,
        )

    tilkjentYtelse.andelerTilkjentYtelse.clear()
    tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelserEtterEUA.map { it.andel })
    return tilkjentYtelse
}

fun Iterable<DeltBosted>.tilEndreteUtebetalingAndeler(): List<EndretUtbetalingAndelMedAndelerTilkjentYtelse> =
    this
        .filter { deltBosted -> deltBosted.fom != null && deltBosted.prosent != null }
        .flatMap { deltBosted ->
            deltBosted.barnPersoner.map {
                lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
                    behandlingId = deltBosted.behandlingId,
                    personer = setOf(it),
                    fom = deltBosted.fom!!,
                    tom = deltBosted.tom,
                    prosent = deltBosted.prosent!!,
                )
            }
        }
