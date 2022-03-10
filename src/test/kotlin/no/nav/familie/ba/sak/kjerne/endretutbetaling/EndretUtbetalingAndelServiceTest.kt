package no.nav.familie.ba.sak.kjerne.endretutbetaling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagEndretUtbetalingAndel
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.tilRestEndretUtbetalingAndel
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

class EndretUtbetalingAndelServiceTest {

    private val mockEndretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    private val mockPersongrunnlagService = mockk<PersongrunnlagService>()
    private val mockPersonopplysningGrunnlagRepository = mockk<PersonopplysningGrunnlagRepository>()
    private val mockAndelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()
    private val mockVilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private lateinit var endretUtbetalingAndelService: EndretUtbetalingAndelService

    @BeforeEach
    fun setup() {
        val beregningService = mockk<BeregningService>()
        val sanityService = mockk<SanityService>()
        endretUtbetalingAndelService = EndretUtbetalingAndelService(
            endretUtbetalingAndelRepository = mockEndretUtbetalingAndelRepository,
            personopplysningGrunnlagRepository = mockPersonopplysningGrunnlagRepository,
            beregningService = beregningService,
            persongrunnlagService = mockPersongrunnlagService,
            andelTilkjentYtelseRepository = mockAndelTilkjentYtelseRepository,
            sanityService = sanityService,
            vilkårsvurderingService = mockVilkårsvurderingService,
            featureToggleService = featureToggleService
        )
    }

    @Test
    fun `Skal kaste feil hvis endringsperiode har årsak delt bosted, men ikke overlapper med delt bosted perioder`() {
        val behandling = lagBehandling()
        val barn = lagPerson(type = PersonType.BARN)
        val endretUtbetalingAndel = lagEndretUtbetalingAndel(
            behandlingId = behandling.id,
            person = barn,
            årsak = Årsak.DELT_BOSTED,
            fom = YearMonth.now().minusMonths(5),
            tom = YearMonth.now().minusMonths(1)
        )
        val restEndretUtbetalingAndel = endretUtbetalingAndel.tilRestEndretUtbetalingAndel()

        val andelerTilkjentYtelse = listOf<AndelTilkjentYtelse>(
            lagAndelTilkjentYtelse(
                person = barn,
                fom = YearMonth.now().minusMonths(10),
                tom = YearMonth.now().plusMonths(5)
            ),
            lagAndelTilkjentYtelse(
                person = barn,
                fom = YearMonth.now().plusMonths(6),
                tom = YearMonth.now().plusMonths(11)
            )
        )

        val vilkårsvurderingUtenDeltBosted = Vilkårsvurdering(
            behandling = behandling
        )
        vilkårsvurderingUtenDeltBosted.personResultater = setOf(
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurderingUtenDeltBosted,
                aktør = barn.aktør,
                resultat = Resultat.OPPFYLT,
                periodeFom = endretUtbetalingAndel.fom?.minusMonths(1)?.førsteDagIInneværendeMåned(),
                periodeTom = LocalDate.now(),
                erDeltBosted = false,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                vilkårType = Vilkår.BOR_MED_SØKER
            )
        )

        every { mockEndretUtbetalingAndelRepository.getById(any()) } returns endretUtbetalingAndel
        every { mockPersongrunnlagService.hentPersonerPåBehandling(any(), behandling) } returns listOf(barn)
        every { mockPersonopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandling.id) } returns lagTestPersonopplysningGrunnlag(behandling.id, barn)
        every { mockAndelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id) } returns andelerTilkjentYtelse
        every { mockEndretUtbetalingAndelRepository.findByBehandlingId(behandlingId = behandling.id) } returns emptyList()
        every { mockVilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id) } returns vilkårsvurderingUtenDeltBosted
        every { featureToggleService.isEnabled(FeatureToggleConfig.DELT_BOSTED_VALIDERING) } returns true

        val feil = assertThrows<FunksjonellFeil> { endretUtbetalingAndelService.oppdaterEndretUtbetalingAndelOgOppdaterTilkjentYtelse(behandling = behandling, endretUtbetalingAndelId = endretUtbetalingAndel.id, restEndretUtbetalingAndel = restEndretUtbetalingAndel) }
        Assertions.assertEquals("Du har valgt årsaken 'delt bosted', denne samstemmer ikke med vurderingene gjort på vilkårsvurderingssiden i perioden du har valgt.", feil.frontendFeilmelding)
    }
}
