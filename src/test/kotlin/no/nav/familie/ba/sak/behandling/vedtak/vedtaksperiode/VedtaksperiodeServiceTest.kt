package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.behandling.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.tilbakekreving.TilbakekrevingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles(
        "postgres",
        "mock-totrinnkontroll",
        "mock-brev-klient",
        "mock-økonomi",
        "mock-pdl",
        "mock-infotrygd-feed",
        "mock-tilbakekreving-klient",
        "mock-infotrygd-barnetrygd",
        "mock-task-repository",
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VedtaksperiodeServiceTest(
        @Autowired
        private val stegService: StegService,

        @Autowired
        private val vedtakService: VedtakService,

        @Autowired
        private val vedtaksperiodeRepository: VedtaksperiodeRepository,

        @Autowired
        private val persongrunnlagService: PersongrunnlagService,

        @Autowired
        private val fagsakService: FagsakService,

        @Autowired
        private val vilkårsvurderingService: VilkårsvurderingService,

        @Autowired
        private val tilbakekrevingService: TilbakekrevingService,

        @Autowired
        private val vedtaksperiodeService: VedtaksperiodeService,

        @Autowired
        private val databaseCleanupService: DatabaseCleanupService
) {

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal ikke kunne lagre flere vedtaksperioder med samme periode og type`() {
        val behandling = kjørStegprosessForFGB(
                tilSteg = StegType.REGISTRERE_SØKNAD,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(randomFnr()),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        val fom = inneværendeMåned().minusMonths(12).førsteDagIInneværendeMåned()
        val tom = inneværendeMåned().sisteDagIInneværendeMåned()
        val type = Vedtaksperiodetype.FORTSATT_INNVILGET
        val vedtaksperiode = VedtaksperiodeMedBegrunnelser(
                behandling = behandling,
                fom = fom,
                tom = tom,
                type = type
        )
        vedtaksperiodeRepository.save(vedtaksperiode)

        val vedtaksperiodeMedSammePeriode = VedtaksperiodeMedBegrunnelser(
                behandling = behandling,
                fom = fom,
                tom = tom,
                type = type
        )
        val feil = assertThrows<DataIntegrityViolationException> { vedtaksperiodeRepository.save(vedtaksperiodeMedSammePeriode) }
        assertTrue(feil.message!!.contains("constraint [vedtaksperiode_fk_behandling_id_fom_tom_type_key]"))
    }

    @Test
    fun `Skal lagre vedtaksperioder ved fortsatt innvilget som resultat`() {
        val søkerFnr = randomFnr()
        kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        val revurdering = kjørStegprosessForRevurderingÅrligKontroll(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                vedtakService = vedtakService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        assertEquals(BehandlingResultat.FORTSATT_INNVILGET, revurdering.resultat)

        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(revurdering)

        assertEquals(1, vedtaksperioder.size)
        assertEquals(Vedtaksperiodetype.FORTSATT_INNVILGET, vedtaksperioder.first().type)
    }
}