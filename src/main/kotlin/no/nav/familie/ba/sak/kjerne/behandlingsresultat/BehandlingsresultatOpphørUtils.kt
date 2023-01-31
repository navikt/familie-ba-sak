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
        val nåværendeBehandlingOpphørsdato = nåværendeAndeler.utledOpphørsdatoForNåværendeBehandlingMedFallback(forrigeAndeler = forrigeAndeler) ?: return Opphørsresultat.IKKE_OPPHØRT
        val forrigeBehandlingOpphørsdato = forrigeAndeler.finnOpphørsdato() ?: YearMonth.now()
        val dagensDato = YearMonth.now()

        return when {
            // Rekkefølgen av sjekkene er viktig for å komme fram til riktig opphørsresultat.
            nåværendeBehandlingOpphørsdato > dagensDato -> Opphørsresultat.IKKE_OPPHØRT
            forrigeBehandlingOpphørsdato > dagensDato || forrigeBehandlingOpphørsdato > nåværendeBehandlingOpphørsdato -> Opphørsresultat.OPPHØRT
            else -> Opphørsresultat.FORTSATT_OPPHØRT
        }
    }

    internal fun List<AndelTilkjentYtelse>.finnOpphørsdato() = this.maxOfOrNull { it.stønadTom }

    /**
     * Hvis opphørsdato ikke finnes i denne behandlingen så ønsker vi å bruke tidligste fom-dato fra forrige behandling
     * Ingen opphørsdato i denne behandlingen skjer kun hvis det ikke finnes noen andeler, og da har vi to scenarier:
     * 1. Ingen andeler i denne behandlingen, men andeler i forrige behandling. Da ønsker vi at opphørsdatoen i denne behandlingen skal være "første endring" som altså er lik tidligste fom-dato
     * 2. Ingen andeler i denne behandlingen, ingen andeler i forrige behandling. Da vil denne funksjonen returnere null
     */
    internal fun List<AndelTilkjentYtelse>.utledOpphørsdatoForNåværendeBehandlingMedFallback(forrigeAndeler: List<AndelTilkjentYtelse>): YearMonth? {
        return this.finnOpphørsdato() ?: forrigeAndeler.minOfOrNull { it.stønadFom }
    }

    /**
     * Hvis det ikke fantes noen andeler i forrige behandling defaulter vi til inneværende måned
     */
    private fun List<AndelTilkjentYtelse>.utledOpphørsdatoForForrigeBehandling(): YearMonth = this.finnOpphørsdato() ?: YearMonth.now()
}
