package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

internal class UtbetalingsoppdragUtilsKtTest {

    @Test
    fun `nasjonalt utbetalingsoppdrag må ha utbetalingsperiode`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag()
        assertThrows<FunksjonellFeil> {
            utbetalingsoppdrag.validerForNullutbetaling(
                Behandlingsresultat.INNVILGET,
                BehandlingKategori.NASJONAL,
                false
            )
        }
    }

    @Test
    fun `innvilget EØS-utbetalingsoppdrag kan mangle utbetalingsperiode, hvis det da er nullutbetaling`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag()
        assertDoesNotThrow {
            utbetalingsoppdrag.validerForNullutbetaling(
                Behandlingsresultat.INNVILGET,
                BehandlingKategori.EØS,
                false
            )
        }
    }

    @Test
    fun `avslått EØS-utbetalingsoppdrag kan ikke ha nullutbetling, og dermed mangle nullutbetaling`() {
        val utbetalingsoppdrag = lagUtbetalingsoppdrag()
        assertThrows<FunksjonellFeil> {
            utbetalingsoppdrag.validerForNullutbetaling(
                Behandlingsresultat.AVSLÅTT,
                BehandlingKategori.EØS,
                false
            )
        }
    }

    private fun lagUtbetalingsoppdrag() = Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.NY,
        "BA",
        "",
        UUID.randomUUID().toString(),
        "",
        LocalDateTime.now(),
        listOf()
    )
}
