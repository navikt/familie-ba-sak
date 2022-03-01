package no.nav.familie.ba.sak.kjerne.beregning

import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.TestUtil
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class AndelTilkjentYtelseServiceTest {

    private lateinit var andelTilkjentYtelseService: AndelTilkjentYtelseService

    val behandling = lagBehandling()
    val søkerPerson = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(35))
    val barnPerson = tilfeldigPerson(fødselsdato = LocalDate.now().minusYears(1))

    val andelTilkjentYtelse = lagAndelTilkjentYtelse(
        behandling = behandling,
        person = søkerPerson,
        fom = YearMonth.now().minusMonths(4),
        tom = YearMonth.now().plusMonths(1)
    )

    val vilkårsvurdering = Vilkårsvurdering(
        behandling = behandling
    )
    val personResultat = PersonResultat(
        vilkårsvurdering = vilkårsvurdering,
        aktør = barnPerson.aktør
    )

    @BeforeEach
    fun setUp() {
        personResultat.vilkårResultater.addAll(
            listOf(
                lagVilkårResultat(Vilkår.BOSATT_I_RIKET, Regelverk.EØS_FORORDNINGEN, TestUtil.feb(2022), null),
                lagVilkårResultat(Vilkår.LOVLIG_OPPHOLD, Regelverk.EØS_FORORDNINGEN, TestUtil.apr(2022), null),
                lagVilkårResultat(Vilkår.BOR_MED_SØKER, Regelverk.EØS_FORORDNINGEN, TestUtil.aug(2022), null),
            )
        )

        vilkårsvurdering.personResultater = setOf(personResultat)
    }

    @Test
    fun `EØS-forordning om alle relevante vilkår er satt til regelverk EØS forordning`() {
        mockkVilkårsvurdering()

        val regelverk = andelTilkjentYtelseService.vurdertEtter(
            andelTilkjentYtelse
        )

        assertEquals(Regelverk.EØS_FORORDNINGEN, regelverk)
    }

    @Test
    fun `Nasjonale regler om alle relevante vilkår er satt til regelverk nasonale regler`() {
        mockkVilkårsvurdering(Regelverk.NASJONALE_REGLER, Regelverk.NASJONALE_REGLER, Regelverk.NASJONALE_REGLER)

        val regelverk = andelTilkjentYtelseService.vurdertEtter(
            andelTilkjentYtelse
        )

        assertEquals(Regelverk.NASJONALE_REGLER, regelverk)
    }

    @Test
    fun `Kaste feil om alle relevante vilkår er satt til forskjellig regelverk`() {
        mockkVilkårsvurdering(Regelverk.NASJONALE_REGLER, Regelverk.NASJONALE_REGLER, Regelverk.EØS_FORORDNINGEN)

        assertThrows<IllegalStateException> {
            andelTilkjentYtelseService.vurdertEtter(andelTilkjentYtelse)
        }
    }

    private fun mockkVilkårsvurdering(
        regelVerkBosattIRiket: Regelverk = Regelverk.EØS_FORORDNINGEN,
        regelVerkLovligOpphold: Regelverk = Regelverk.EØS_FORORDNINGEN,
        regelVerkBorMedSøker: Regelverk = Regelverk.EØS_FORORDNINGEN
    ) {
        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling
        )

        val barnPersonResultat = PersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            aktør = barnPerson.aktør
        )

        val vilkårResultat = listOf(
            lagVilkårResultat(Vilkår.BOSATT_I_RIKET, regelVerkBosattIRiket, YearMonth.now().minusYears(1), null),
            lagVilkårResultat(Vilkår.LOVLIG_OPPHOLD, regelVerkLovligOpphold, YearMonth.now().minusYears(1), null),
            lagVilkårResultat(Vilkår.BOR_MED_SØKER, regelVerkBorMedSøker, YearMonth.now().minusYears(1), null),
            lagVilkårResultat(
                Vilkår.UNDER_18_ÅR,
                Regelverk.NASJONALE_REGLER,
                YearMonth.now().minusYears(1),
                YearMonth.now().plusYears(17)
            ),
        )
        barnPersonResultat.vilkårResultater.addAll(vilkårResultat)
        vilkårsvurdering.personResultater = setOf(barnPersonResultat)

        val vilkårsvurderingService = mockk<VilkårsvurderingService>()
        andelTilkjentYtelseService = AndelTilkjentYtelseService(vilkårsvurderingService)

        andelTilkjentYtelseService = AndelTilkjentYtelseService(vilkårsvurderingService)
        barnPersonResultat.vilkårResultater.addAll(vilkårResultat)

        every { vilkårsvurderingService.hentAktivForBehandling(andelTilkjentYtelse.behandlingId) } returns vilkårsvurdering
    }
}
