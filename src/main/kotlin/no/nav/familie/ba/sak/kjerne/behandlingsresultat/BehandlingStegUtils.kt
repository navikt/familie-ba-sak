package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.steg.BehandlingsresultatSteg
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import java.time.YearMonth

fun Tidslinje<Boolean>.kastFeilVedEndringEtter(
    migreringsdatoForrigeIverksatteBehandling: YearMonth,
    behandling: Behandling,
) {
    val endringIUtbetalingEtterDato =
        tilPerioder()
            .filter { it.tom == null || it.tom!!.toYearMonth().isSameOrAfter(migreringsdatoForrigeIverksatteBehandling) }

    val erEndringIUtbetalingEtterMigreringsdato = endringIUtbetalingEtterDato.any { it.verdi == true }

    if (erEndringIUtbetalingEtterMigreringsdato) {
        BehandlingsresultatSteg.logger.warn("Feil i behandling $behandling.\n\nEndring i måned ${endringIUtbetalingEtterDato.first { it.verdi == true }.fom?.toYearMonth()}.")
        throw FunksjonellFeil(
            "Det finnes endringer i behandlingen som har økonomisk konsekvens for bruker." +
                "Det skal ikke skje for endre migreringsdatobehandlinger." +
                "Endringer må gjøres i en separat behandling.",
            "Det finnes endringer i behandlingen som har økonomisk konsekvens for bruker." +
                "Det skal ikke skje for endre migreringsdatobehandlinger." +
                "Endringer må gjøres i en separat behandling.",
        )
    }
}
