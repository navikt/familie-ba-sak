package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.mockk
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurderingFraScenarioDto
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioPersonDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.stubScenario
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForBehandling
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class RevurderingDødsfallTest(
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractVerdikjedetest() {
    @Test
    fun `Dødsfall bruker skal kjøre gjennom`() {
        val scenario =
            ScenarioDto(
                søker =
                    ScenarioPersonDto(
                        fødselsdato = "1982-01-12",
                        fornavn = "Mor",
                        etternavn = "Søker",
                    ),
                barna =
                    listOf(
                        ScenarioPersonDto(
                            fødselsdato = LocalDate.now().minusMonths(2).toString(),
                            fornavn = "Barn",
                            etternavn = "Barnesen",
                        ),
                    ),
            ).also { stubScenario(it) }

        behandleFødselshendelse(
            nyBehandlingHendelse =
                NyBehandlingHendelse(
                    morsIdent = scenario.søker.ident,
                    barnasIdenter = listOf(scenario.barna.first().ident),
                ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService,
            brevmalService = brevmalService,
        )

        val overstyrendeVilkårResultater =
            scenario.barna.associate { it.aktørId to emptyList<VilkårResultat>() }.toMutableMap()

        // Ved søkers dødsfall settes tomdatoen for "bosatt i riket"-vilkåret til dagen søker døde.
        overstyrendeVilkårResultater[scenario.søker.aktørId] =
            listOf(
                lagVilkårResultat(
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = LocalDate.parse(scenario.søker.fødselsdato),
                    periodeTom = LocalDate.now().minusMonths(1),
                    personResultat = mockk(relaxed = true),
                ),
                lagVilkårResultat(
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    periodeFom = LocalDate.parse(scenario.søker.fødselsdato),
                    periodeTom = LocalDate.now().minusMonths(1),
                    personResultat = mockk(relaxed = true),
                ),
            )

        val behandlingDødsfall =
            kjørStegprosessForBehandling(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = scenario.søker.ident,
                barnasIdenter = listOf(scenario.barna.first().ident),
                vedtakService = vedtakService,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingÅrsak = BehandlingÅrsak.DØDSFALL_BRUKER,
                overstyrendeVilkårsvurdering = lagVilkårsvurderingFraScenarioDto(scenario, overstyrendeVilkårResultater),
                behandlingstype = BehandlingType.REVURDERING,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                fagsakService = fagsakService,
                brevmalService = brevmalService,
            )

        val fagsakDtoEtterBehandlingAvsluttet =
            familieBaSakKlient().hentFagsak(fagsakId = behandlingDødsfall.fagsak.id)

        generellAssertFagsak(
            fagsakDto = fagsakDtoEtterBehandlingAvsluttet,
            fagsakStatus = FagsakStatus.AVSLUTTET,
            behandlingStegType = StegType.BEHANDLING_AVSLUTTET,
            aktivBehandlingId = behandlingDødsfall.id,
        )
    }

    @Test
    fun `Dødsfall bruker skal stoppes dersom ikke bosatt i riket er stoppet før dagens dato`() {
        val scenario =
            ScenarioDto(
                søker =
                    ScenarioPersonDto(
                        fødselsdato = "1982-01-12",
                        fornavn = "Mor",
                        etternavn = "Søker",
                    ),
                barna =
                    listOf(
                        ScenarioPersonDto(
                            fødselsdato = LocalDate.now().minusMonths(2).toString(),
                            fornavn = "Barn",
                            etternavn = "Barnesen",
                        ),
                    ),
            ).also { stubScenario(it) }

        behandleFødselshendelse(
            nyBehandlingHendelse =
                NyBehandlingHendelse(
                    morsIdent = scenario.søker.ident,
                    barnasIdenter = listOf(scenario.barna.first().ident),
                ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService,
            brevmalService = brevmalService,
        )

        val overstyrendeVilkårResultater =
            (scenario.barna + scenario.søker).associate { it.aktørId to emptyList<VilkårResultat>() }.toMutableMap()

        assertThrows<FunksjonellFeil> {
            kjørStegprosessForBehandling(
                tilSteg = StegType.BEHANDLINGSRESULTAT,
                søkerFnr = scenario.søker.ident,
                barnasIdenter =
                    listOf(
                        scenario.barna.first().ident,
                    ),
                vedtakService = vedtakService,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingÅrsak = BehandlingÅrsak.DØDSFALL_BRUKER,
                overstyrendeVilkårsvurdering =
                    lagVilkårsvurderingFraScenarioDto(
                        scenario,
                        overstyrendeVilkårResultater,
                    ),
                behandlingstype = BehandlingType.REVURDERING,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                fagsakService = fagsakService,
                brevmalService = brevmalService,
            )
        }
    }
}
