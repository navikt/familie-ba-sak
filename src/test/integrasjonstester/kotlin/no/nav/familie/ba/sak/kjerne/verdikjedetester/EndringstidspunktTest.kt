package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.mockk
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurderingFraScenarioDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioPersonDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.stubScenario
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForBehandling
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class EndringstidspunktTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractVerdikjedetest() {
    @Test
    fun `Skal filtrere bort alle vedtaksperioder før endringstidspunktet`() {
        val barnFødselsdato = LocalDate.now().minusYears(2)
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
                            fødselsdato = barnFødselsdato.toString(),
                            fornavn = "Barn",
                            etternavn = "Barnesen",
                        ),
                    ),
            ).also { stubScenario(it) }

        val overstyrendeVilkårResultaterFGB =
            scenario.barna.associate { it.aktørId to emptyList<VilkårResultat>() }.toMutableMap()

        overstyrendeVilkårResultaterFGB[scenario.søker.aktørId] = emptyList()

        kjørStegprosessForBehandling(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = scenario.søker.ident,
            barnasIdenter = listOf(scenario.barna.first().ident),
            vedtakService = vedtakService,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            overstyrendeVilkårsvurdering =
                lagVilkårsvurderingFraScenarioDto(
                    scenario,
                    overstyrendeVilkårResultaterFGB,
                ),
            behandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            fagsakService = fagsakService,
            brevmalService = brevmalService,
        )

        val sisteDagUtenDeltBostedOppfylt = barnFødselsdato.plusYears(1).sisteDagIMåned()
        val førsteDagMedDeltBostedOppfylt = sisteDagUtenDeltBostedOppfylt.førsteDagINesteMåned()
        val sisteDagMedDeltBosdet = sisteDagUtenDeltBostedOppfylt.plusMonths(2).sisteDagIMåned()

        val overstyrendeVilkårResultaterRevurdering =
            scenario.barna
                .associate {
                    it.aktørId to
                        listOf(
                            lagVilkårResultat(
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                periodeFom = barnFødselsdato,
                                periodeTom = førsteDagMedDeltBostedOppfylt,
                                personResultat = mockk(relaxed = true),
                            ),
                            lagVilkårResultat(
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                periodeFom = førsteDagMedDeltBostedOppfylt,
                                periodeTom = sisteDagMedDeltBosdet,
                                personResultat = mockk(relaxed = true),
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                            ),
                            lagVilkårResultat(
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                periodeFom = førsteDagMedDeltBostedOppfylt,
                                periodeTom = sisteDagMedDeltBosdet.førsteDagINesteMåned(),
                                personResultat = mockk(relaxed = true),
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED),
                            ),
                        )
                }.toMutableMap()

        overstyrendeVilkårResultaterRevurdering[scenario.søker.aktørId] = emptyList()

        val revurdering =
            kjørStegprosessForBehandling(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = scenario.søker.ident,
                barnasIdenter = listOf(scenario.barna.first().ident),
                vedtakService = vedtakService,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                overstyrendeVilkårsvurdering =
                    lagVilkårsvurderingFraScenarioDto(
                        scenario,
                        overstyrendeVilkårResultaterRevurdering,
                    ),
                behandlingstype = BehandlingType.REVURDERING,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                fagsakService = fagsakService,
                brevmalService = brevmalService,
            )

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        val førsteFomDatoIVedtaksperiodene =
            vedtaksperioder.minOf { it.fom ?: throw Feil("Ingen fom-dato") }

        assertTrue(
            førsteFomDatoIVedtaksperiodene.isEqual(
                førsteDagMedDeltBostedOppfylt.førsteDagINesteMåned(),
            ),
        )
    }
}
