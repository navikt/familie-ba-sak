package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class UtbetalingsoppdragUtilsTest {
    @Test
    fun `Skal ikke kunne iverksette et vedtak uten endringer og som ikke har endringsutbetalinger`() {
        val feil = assertThrows<FunksjonellFeil> {
            Utbetalingsoppdrag(
                aktoer = "",
                avstemmingTidspunkt = LocalDateTime.now(),
                fagSystem = "BA",
                kodeEndring = Utbetalingsoppdrag.KodeEndring.ENDR,
                saksbehandlerId = "",
                saksnummer = "",
                utbetalingsperiode = emptyList(),
            ).valider(
                behandlingsresultat = BehandlingResultat.FORTSATT_INNVILGET,
                harAndelTilkjentYtelseMedEndringsutbetalinger = false
            )
        }
        assertTrue(
            feil.message!!.contains("Utbetalingsoppdraget inneholder ingen utbetalingsperioder")
        )
    }

    @Test
    fun `Skal kunne iverksette et vedtak uten endringer men som har endringsutbetalinger`() {
        Utbetalingsoppdrag(
            aktoer = "",
            avstemmingTidspunkt = LocalDateTime.now(),
            fagSystem = "BA",
            kodeEndring = Utbetalingsoppdrag.KodeEndring.ENDR,
            saksbehandlerId = "",
            saksnummer = "",
            utbetalingsperiode = emptyList(),
        ).valider(
            behandlingsresultat = BehandlingResultat.FORTSATT_INNVILGET,
            harAndelTilkjentYtelseMedEndringsutbetalinger = true
        )
    }

    @Test
    fun `Skal ikke kunne iverksette et vedtak med endringer og fortsatt innvilget behandlingsresultat`() {
        val feil = assertThrows<FunksjonellFeil> {
            Utbetalingsoppdrag(
                aktoer = "",
                avstemmingTidspunkt = LocalDateTime.now(),
                fagSystem = "BA",
                kodeEndring = Utbetalingsoppdrag.KodeEndring.ENDR,
                saksbehandlerId = "",
                saksnummer = "",
                utbetalingsperiode = listOf(mockk()),
            ).valider(
                behandlingsresultat = BehandlingResultat.FORTSATT_INNVILGET,
                harAndelTilkjentYtelseMedEndringsutbetalinger = false
            )
        }
        assertTrue(
            feil.message!!.contains("Behandling har resultat fortsatt innvilget")
        )
    }
}
