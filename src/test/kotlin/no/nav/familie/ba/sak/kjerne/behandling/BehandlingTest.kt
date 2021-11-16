package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class BehandlingTest {

    @Test
    fun `validerBehandling kaster feil hvis behandlingType og behandlingÅrsak ikke samsvarer ved teknisk endring`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.TEKNISK_ENDRING,
            årsak = BehandlingÅrsak.SØKNAD
        )
        assertThrows<RuntimeException> { behandling.validerBehandlingstype() }
    }

    @Test
    fun `validerBehandling kaster feil hvis behandlingType er teknisk opphør`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.TEKNISK_OPPHØR,
            årsak = BehandlingÅrsak.TEKNISK_OPPHØR
        )
        assertThrows<RuntimeException> { behandling.validerBehandlingstype() }
    }

    @Test
    fun `erRentTekniskOpphør kastet feil hvis behandlingType og behandlingÅrsak ikke samsvarer ved teknisk opphør`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.TEKNISK_OPPHØR,
            årsak = BehandlingÅrsak.SØKNAD
        )
        assertThrows<RuntimeException> { behandling.erTekniskOpphør() }
    }

    @Test
    fun `erRentTekniskOpphør gir true når teknisk opphør`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.TEKNISK_OPPHØR,
            årsak = BehandlingÅrsak.TEKNISK_OPPHØR
        )
        assertTrue(behandling.erTekniskOpphør())
    }

    @Test
    fun `erRentTekniskOpphør gir false når ikke teknisk opphør`() {
        val behandling = lagBehandling(
            behandlingType = BehandlingType.REVURDERING,
            årsak = BehandlingÅrsak.SØKNAD
        )
        assertFalse(behandling.erTekniskOpphør())
    }

    @Test
    fun `Skal velge ordinær ved FGB`() {
        assertEquals(
            BehandlingUnderkategori.ORDINÆR,
            Behandlingutils.bestemUnderkategori(
                nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                nyBehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                løpendeUnderkategori = null
            )
        )
    }

    @Test
    fun `Skal velge utvidet ved FGB`() {
        assertEquals(
            BehandlingUnderkategori.UTVIDET,
            Behandlingutils.bestemUnderkategori(
                nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                nyBehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                løpendeUnderkategori = null
            )
        )
    }

    @Test
    fun `Skal velge utvidet ved RV når FGB er utvidet`() {
        assertEquals(
            BehandlingUnderkategori.UTVIDET,
            Behandlingutils.bestemUnderkategori(
                nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                nyBehandlingType = BehandlingType.REVURDERING,
                løpendeUnderkategori = BehandlingUnderkategori.UTVIDET
            )
        )
    }

    @Test
    fun `Skal velge ordinær ved RV når FGB er ordinær`() {
        assertEquals(
            BehandlingUnderkategori.ORDINÆR,
            Behandlingutils.bestemUnderkategori(
                nyUnderkategori = BehandlingUnderkategori.ORDINÆR,
                nyBehandlingType = BehandlingType.REVURDERING,
                løpendeUnderkategori = BehandlingUnderkategori.ORDINÆR
            )
        )
    }

    @Test
    fun `Skal velge utvidet ved RV når FGB er ordinær`() {
        assertEquals(
            BehandlingUnderkategori.UTVIDET,
            Behandlingutils.bestemUnderkategori(
                nyUnderkategori = BehandlingUnderkategori.UTVIDET,
                nyBehandlingType = BehandlingType.REVURDERING,
                løpendeUnderkategori = BehandlingUnderkategori.ORDINÆR
            )
        )
    }

    @Test
    fun `Skal returnere utvidet hvis det eksisterer en løpende utvidet-sak`() {
        val søker = randomFnr()
        val søkerAktørId = randomAktørId()

        val behandling = lagBehandling()

        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now()
        )

        val andelTilkjentYtelse = listOf(
            AndelTilkjentYtelse(
                behandlingId = behandling.id,
                type = YtelseType.UTVIDET_BARNETRYGD,
                tilkjentYtelse = tilkjentYtelse,
                personIdent = søker,
                aktørId = søkerAktørId,
                kalkulertUtbetalingsbeløp = 1054,
                sats = 123,
                stønadFom = YearMonth.of(2015, 6),
                stønadTom = YearMonth.now().plusYears(5),
                prosent = BigDecimal(2)
            )
        )

        val løpendeUndekategori = Behandlingutils.utledLøpendeUnderkategori(andelTilkjentYtelse)

        assertEquals(BehandlingUnderkategori.UTVIDET, løpendeUndekategori)
    }

    @Test
    fun `Skal returnere ordinær hvis det eksisterer en utvidet-sak som er avsluttet`() {
        val søker = randomFnr()
        val søkerAktørId = randomAktørId()

        val behandling = lagBehandling()

        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now()
        )

        val andelTilkjentYtelse = listOf(
            AndelTilkjentYtelse(
                behandlingId = behandling.id,
                type = YtelseType.UTVIDET_BARNETRYGD,
                tilkjentYtelse = tilkjentYtelse,
                personIdent = søker,
                aktørId = søkerAktørId,
                kalkulertUtbetalingsbeløp = 1054,
                sats = 123,
                stønadFom = YearMonth.of(2015, 6),
                stønadTom = YearMonth.now().minusYears(1),
                prosent = BigDecimal(2)
            )
        )

        val løpendeUndekategori = Behandlingutils.utledLøpendeUnderkategori(andelTilkjentYtelse)

        assertEquals(BehandlingUnderkategori.ORDINÆR, løpendeUndekategori)
    }

    @Test
    fun `Skal returnere ordinær hvis det eksisterer en løpende ordinær-sak`() {
        val søker = randomFnr()
        val søkerAktørId = randomAktørId()

        val behandling = lagBehandling()

        val tilkjentYtelse = TilkjentYtelse(
            behandling = behandling,
            opprettetDato = LocalDate.now(),
            endretDato = LocalDate.now()
        )

        val andelTilkjentYtelse = listOf(
            AndelTilkjentYtelse(
                behandlingId = behandling.id,
                type = YtelseType.ORDINÆR_BARNETRYGD,
                tilkjentYtelse = tilkjentYtelse,
                personIdent = søker,
                aktørId = søkerAktørId,
                kalkulertUtbetalingsbeløp = 1054,
                sats = 123,
                stønadFom = YearMonth.of(2015, 6),
                stønadTom = YearMonth.now().plusYears(2),
                prosent = BigDecimal(2)
            )
        )

        val løpendeUndekategori = Behandlingutils.utledLøpendeUnderkategori(andelTilkjentYtelse)

        assertEquals(BehandlingUnderkategori.ORDINÆR, løpendeUndekategori)
    }
}
