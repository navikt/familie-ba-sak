package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.overstyring.domene.OverstyrtUtbetaling
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

internal class TilkjentYtelseUtilsEndretUtbetalingAndelTest {

    private val featureToggleService = mockk<FeatureToggleService>()

    @BeforeEach
    fun setUp() {
        every { featureToggleService.isEnabled(any()) } answers { true }
    }

    @Test
    fun `Ba`() {
        val barnFødselsdato = LocalDate.of(2016, 2, 2)
        val barnSeksårsdag = barnFødselsdato.plusYears(6)

        val behandling = lagBehandling()
        val tilkjentYtelse =
            TilkjentYtelse(behandling = behandling, endretDato = LocalDate.now(), opprettetDato = LocalDate.now())

        val andelerTilkjentYtelser = listOf(
            AndelTilkjentYtelse(
                behandlingId = behandling.id,
                tilkjentYtelse = tilkjentYtelse,
                personIdent = "1234",
                beløp = 100,
                stønadFom = YearMonth.now(),
                stønadTom = YearMonth.now(),
                type = YtelseType.ORDINÆR_BARNETRYGD
            )
        )

        val endretUtbetalinger = listOf<OverstyrtUtbetaling>(OverstyrtUtbetaling())

        TilkjentYtelseUtils.oppdaterTilkjentYtelseMedEndretUtbetalingAndel(andelerTilkjentYtelser, endretUtbetalinger)

        assertEquals(1, tilkjentYtelse.andelerTilkjentYtelse.size)

        val andelTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.first()
        assertEquals(
            MånedPeriode(
                barnSeksårsdag.nesteMåned(),
                barnFødselsdato.plusYears(18).forrigeMåned()
            ),
            MånedPeriode(andelTilkjentYtelse.stønadFom, andelTilkjentYtelse.stønadTom)
        )
    }
}