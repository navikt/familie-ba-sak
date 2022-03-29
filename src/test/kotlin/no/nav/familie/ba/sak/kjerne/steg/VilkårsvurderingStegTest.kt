package no.nav.familie.ba.sak.kjerne.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class VilkårsvurderingStegTest {

    private val vilkårService: VilkårService = mockk()
    private val beregningService: BeregningService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val tilbakestillBehandlingService: TilbakestillBehandlingService = mockk()
    private val kompetanseService: KompetanseService = mockk()
    private val featureToggleService: FeatureToggleService = mockk()

    private val vilkårsvurderingSteg: VilkårsvurderingSteg = VilkårsvurderingSteg(
        vilkårService,
        beregningService,
        persongrunnlagService,
        behandlingService,
        tilbakestillBehandlingService,
        kompetanseService,
        featureToggleService
    )

    val behandling = lagBehandling(
        behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD,
        årsak = BehandlingÅrsak.HELMANUELL_MIGRERING
    )
    val søkerPersonIdent = randomFnr()
    val søkerAktørId = randomAktørId(søkerPersonIdent)
    val barnIdent = randomFnr()
    val barnAktørId = randomAktørId(barnIdent)

    @BeforeEach
    fun setup() {
        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerPersonIdent,
            barnasIdenter = listOf(barnIdent)
        )
        every { tilbakestillBehandlingService.tilbakestillDataTilVilkårsvurderingssteg(behandling) } returns Unit
        every { beregningService.oppdaterBehandlingMedBeregning(any(), any()) } returns lagInitiellTilkjentYtelse(
            behandling
        )

        every { kompetanseService.tilpassKompetanserTilRegelverk(behandling.id) } returns emptyList()
        every { featureToggleService.isEnabled(any()) } returns true
    }

    @Test
    fun `skal ikke fortsette til neste steg når helmanuell migreringsbehandling ikke har del bosted`() {
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = søkerAktørId,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(1984, 1, 1),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER,
            erDeltBosted = false
        )
        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = barnAktørId,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(2019, 1, 1),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = false
        )
        vikårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vikårsvurdering

        val exception = assertThrows<RuntimeException> { vilkårsvurderingSteg.utførStegOgAngiNeste(behandling, "") }
        assertEquals(
            "Behandling ${behandling.id} kan ikke fortsettes uten delt bosted " +
                "i vilkårsvurdering for minst ett av barna",
            exception.message
        )
    }

    @Test
    fun `skal fortsette til neste steg når helmanuell migreringsbehandling har del bosted`() {
        val vikårsvurdering = Vilkårsvurdering(behandling = behandling)
        val søkerPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = søkerAktørId,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(1984, 1, 1),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.SØKER,
            erDeltBosted = false
        )
        val barnPersonResultat = lagPersonResultat(
            vilkårsvurdering = vikårsvurdering,
            aktør = barnAktørId,
            resultat = Resultat.OPPFYLT,
            periodeFom = LocalDate.of(2019, 1, 1),
            periodeTom = LocalDate.now(),
            lagFullstendigVilkårResultat = true,
            personType = PersonType.BARN,
            erDeltBosted = true
        )
        vikårsvurdering.personResultater = setOf(søkerPersonResultat, barnPersonResultat)
        every { vilkårService.hentVilkårsvurderingThrows(behandling.id) } returns vikårsvurdering

        assertDoesNotThrow { vilkårsvurderingSteg.utførStegOgAngiNeste(behandling, "") }
    }
}
