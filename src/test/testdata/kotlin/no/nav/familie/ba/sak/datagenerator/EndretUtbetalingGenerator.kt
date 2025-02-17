package no.nav.familie.ba.sak.datagenerator

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.EndretUtbetalingAndelMedAndelerTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

fun lagEndretUtbetalingAndel(
    behandlingId: Long,
    barn: Person,
    fom: YearMonth,
    tom: YearMonth,
    prosent: Int,
) = lagEndretUtbetalingAndel(
    behandlingId = behandlingId,
    person = barn,
    fom = fom,
    tom = tom,
    prosent = BigDecimal(prosent),
)

fun lagEndretUtbetalingAndel(
    id: Long = 0,
    behandlingId: Long = 0,
    person: Person,
    prosent: BigDecimal = BigDecimal.valueOf(100),
    fom: YearMonth = YearMonth.now().minusMonths(1),
    tom: YearMonth? = YearMonth.now(),
    årsak: Årsak = Årsak.DELT_BOSTED,
    avtaletidspunktDeltBosted: LocalDate = LocalDate.now().minusMonths(1),
    søknadstidspunkt: LocalDate = LocalDate.now().minusMonths(1),
) = EndretUtbetalingAndel(
    id = id,
    behandlingId = behandlingId,
    person = person,
    prosent = prosent,
    fom = fom,
    tom = tom,
    årsak = årsak,
    avtaletidspunktDeltBosted = avtaletidspunktDeltBosted,
    søknadstidspunkt = søknadstidspunkt,
    begrunnelse = "Test",
)

fun lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
    behandlingId: Long,
    barn: Person,
    fom: YearMonth,
    tom: YearMonth,
    prosent: Int,
) = lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
    behandlingId = behandlingId,
    person = barn,
    fom = fom,
    tom = tom,
    prosent = BigDecimal(prosent),
)

fun lagEndretUtbetalingAndelMedAndelerTilkjentYtelse(
    id: Long = 0,
    behandlingId: Long = 0,
    person: Person,
    prosent: BigDecimal = BigDecimal.valueOf(100),
    fom: YearMonth = YearMonth.now().minusMonths(1),
    tom: YearMonth? = YearMonth.now(),
    årsak: Årsak = Årsak.DELT_BOSTED,
    avtaletidspunktDeltBosted: LocalDate = LocalDate.now().minusMonths(1),
    søknadstidspunkt: LocalDate = LocalDate.now().minusMonths(1),
    andelTilkjentYtelser: MutableList<AndelTilkjentYtelse> = mutableListOf(),
): EndretUtbetalingAndelMedAndelerTilkjentYtelse {
    val eua =
        EndretUtbetalingAndel(
            id = id,
            behandlingId = behandlingId,
            person = person,
            prosent = prosent,
            fom = fom,
            tom = tom,
            årsak = årsak,
            avtaletidspunktDeltBosted = avtaletidspunktDeltBosted,
            søknadstidspunkt = søknadstidspunkt,
            begrunnelse = "Test",
        )

    return EndretUtbetalingAndelMedAndelerTilkjentYtelse(eua, andelTilkjentYtelser)
}
