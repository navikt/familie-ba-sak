package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.BaseEntitet
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.vilkårsvurdering.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.behandlingstema.BehandlingstemaService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingMetrics
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.full.declaredMemberProperties

class VilkårsvurderingForNyBehandlingServiceTest {

    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val behandlingService = mockk<BehandlingService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val behandlingstemaService = mockk<BehandlingstemaService>()
    private val endretUtbetalingAndelService = mockk<EndretUtbetalingAndelService>()
    private val vilkårsvurderingMetrics = mockk<VilkårsvurderingMetrics>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private lateinit var vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService

    @BeforeEach
    fun setUp() {
        vilkårsvurderingForNyBehandlingService = VilkårsvurderingForNyBehandlingService(
            vilkårsvurderingService = vilkårsvurderingService,
            behandlingService = behandlingService,
            persongrunnlagService = persongrunnlagService,
            behandlingstemaService = behandlingstemaService,
            endretUtbetalingAndelService = endretUtbetalingAndelService,
            vilkårsvurderingMetrics = vilkårsvurderingMetrics,
            featureToggleService = featureToggleService,
        )

        every { featureToggleService.isEnabled(FeatureToggleConfig.SATSENDRING_KOPIER_GRUNNLAG_FRA_FORRIGE_BEHANDLING) } returns true
    }

    @Test
    fun `skal kopiere vilkårsvurdering fra forrige behandling hvis satsendring - alle vilkår for alle personer er oppfylt`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)
        val fagsak = Fagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SATSENDRING)
        val forrigeBehandling = lagBehandling(fagsak = fagsak, årsak = BehandlingÅrsak.SØKNAD)

        val forrigeVilkårsvurdering = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = listOf(barn),
            behandling = forrigeBehandling,
            overstyrendeVilkårResultater = emptyMap(),
            id = 1,
        )
        val forventetNåværendeVilkårsvurdering = lagVilkårsvurderingMedOverstyrendeResultater(
            søker = søker,
            barna = listOf(barn),
            behandling = behandling,
            overstyrendeVilkårResultater = emptyMap(),
        )

        every { vilkårsvurderingService.hentAktivForBehandling(behandlingId = forrigeBehandling.id) } returns forrigeVilkårsvurdering

        val slot = slot<Vilkårsvurdering>()

        every { vilkårsvurderingService.lagreNyOgDeaktiverGammel(capture(slot)) } returnsArgument 0

        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandling,
        )

        verify(exactly = 1) { vilkårsvurderingService.lagreNyOgDeaktiverGammel(any()) }

        assertThat(slot.captured.id).isNotEqualTo(forrigeVilkårsvurdering.id)
        assertThat(slot.captured.behandling.id).isNotEqualTo(forrigeVilkårsvurdering.behandling.id)

        val vilkårResultater =
            slot.captured.personResultater.fold(mutableListOf<Pair<List<VilkårResultat>, List<VilkårResultat>>>()) { acc, personResultat ->
                val vilkårResultaterForrigeBehandlingForPerson =
                    forventetNåværendeVilkårsvurdering.personResultater.filter { it.aktør.aktivFødselsnummer() == personResultat.aktør.aktivFødselsnummer() }
                        .flatMap { it.vilkårResultater }
                acc.addAll(
                    personResultat.vilkårResultater.groupBy { it.vilkårType }
                        .map { (vilkårType, vilkårResultater) ->
                            Pair(
                                vilkårResultater,
                                vilkårResultaterForrigeBehandlingForPerson.filter { forrigeVilkårResultat -> forrigeVilkårResultat.vilkårType == vilkårType },
                            )
                        },
                )
                acc
            }

        val baseEntitetFelter =
            BaseEntitet::class.declaredMemberProperties.map { it.name }.toTypedArray()
        vilkårResultater.forEach {
            assertThat(it.first).usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                "personResultat",
                *baseEntitetFelter,
            )
                .isEqualTo(it.second)
        }
    }
}
