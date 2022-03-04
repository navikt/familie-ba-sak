package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.mockk
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.dataGenerator.behandling.kjørStegprosessForBehandling
import no.nav.familie.ba.sak.dataGenerator.vilkårsvurdering.lagVilkårsvurderingFraRestScenario
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.EndretUtbetalingAndelService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class EndringstidspunktTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val persongrunnlagService: PersongrunnlagService,
    @Autowired private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val endretUtbetalingAndelService: EndretUtbetalingAndelService,
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal filtrere bort alle vedtaksperioder før endringstidspunktet`() {
        val barnFødselsdato = LocalDate.now().minusYears(2)
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(
                    fødselsdato = "1982-01-12",
                    fornavn = "Mor",
                    etternavn = "Søker",
                ),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = barnFødselsdato.toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    )
                )
            )
        )

        val overstyrendeVilkårResultaterFGB =
            scenario.barna.associate { it.aktørId!! to emptyList<VilkårResultat>() }.toMutableMap()

        overstyrendeVilkårResultaterFGB[scenario.søker.aktørId!!] = emptyList()

        kjørStegprosessForBehandling(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = scenario.søker.ident!!,
            barnasIdenter = listOf(scenario.barna.first().ident!!),
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            overstyrendeVilkårsvurdering = lagVilkårsvurderingFraRestScenario(
                scenario,
                overstyrendeVilkårResultaterFGB
            ),
            behandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING,

            vedtakService = vedtakService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            endretUtbetalingAndelService = endretUtbetalingAndelService,
            fagsakService = fagsakService,
            persongrunnlagService = persongrunnlagService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )

        val sisteDagUtenDeltBostedOppfylt = barnFødselsdato.plusYears(1).sisteDagIMåned()
        val førsteDagMedDeltBostedOppfylt = sisteDagUtenDeltBostedOppfylt.førsteDagINesteMåned()

        val overstyrendeVilkårResultaterRevurdering =
            scenario.barna.associate {
                it.aktørId!! to listOf(
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = barnFødselsdato,
                        periodeTom = førsteDagMedDeltBostedOppfylt,
                        personResultat = mockk(relaxed = true),
                    ),
                    lagVilkårResultat(
                        vilkårType = Vilkår.BOR_MED_SØKER,
                        periodeFom = førsteDagMedDeltBostedOppfylt,
                        periodeTom = null,
                        personResultat = mockk(relaxed = true),
                        utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.DELT_BOSTED)
                    ),
                )
            }.toMutableMap()

        overstyrendeVilkårResultaterRevurdering[scenario.søker.aktørId] = emptyList()

        val revurdering = kjørStegprosessForBehandling(
            tilSteg = StegType.BEHANDLING_AVSLUTTET,
            søkerFnr = scenario.søker.ident,
            barnasIdenter = listOf(scenario.barna.first().ident!!),
            underkategori = BehandlingUnderkategori.ORDINÆR,
            behandlingÅrsak = BehandlingÅrsak.SØKNAD,
            overstyrendeVilkårsvurdering = lagVilkårsvurderingFraRestScenario(
                scenario,
                overstyrendeVilkårResultaterRevurdering
            ),
            behandlingstype = BehandlingType.REVURDERING,

            vedtakService = vedtakService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
            endretUtbetalingAndelService = endretUtbetalingAndelService,
            fagsakService = fagsakService,
            persongrunnlagService = persongrunnlagService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        val førsteTomDatoIVedtaksperiodene =
            vedtaksperioder.minOf { it.tom ?: TIDENES_ENDE } ?: error("Fant ingen vedtaksperioder")

        assertTrue(
            førsteTomDatoIVedtaksperiodene.isSameOrAfter(
                førsteDagMedDeltBostedOppfylt.førsteDagINesteMåned()
            )
        )
    }
}
