package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import java.time.YearMonth

internal enum class Opphørsresultat {
    OPPHØRT,
    FORTSATT_OPPHØRT,
    IKKE_OPPHØRT
}

object BehandlingsresultatOpphørUtils {

    internal fun hentOpphørsresultatPåBehandling(
        nåværendeAndeler: List<AndelTilkjentYtelse>,
        forrigeAndeler: List<AndelTilkjentYtelse>
    ): Opphørsresultat {
        val nåværendeBehandlingOpphørsdato = nåværendeAndeler.maxOf { it.stønadTom }
        val forrigeBehandlingOpphørsdato = forrigeAndeler.maxOf { it.stønadTom }
        val dagensDato = YearMonth.now()

        return when {
            // Rekkefølgen av sjekkene er viktig for å komme fram til riktig opphørsresultat.
            nåværendeBehandlingOpphørsdato > dagensDato -> Opphørsresultat.IKKE_OPPHØRT
            forrigeBehandlingOpphørsdato > dagensDato || forrigeBehandlingOpphørsdato > nåværendeBehandlingOpphørsdato -> Opphørsresultat.OPPHØRT
            else -> Opphørsresultat.FORTSATT_OPPHØRT
        }
    }
}