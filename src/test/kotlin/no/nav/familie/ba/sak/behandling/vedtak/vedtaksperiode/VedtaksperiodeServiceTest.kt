package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.RestPutVedtaksperiodeMedBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.RestVedtaksbegrunnelse
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.behandling.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.Feil
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

    val søkerFnr = randomFnr()
    val barnFnr = ClientMocks.barnFnr[0]
    var førstegangsbehandling: Behandling? = null
    var revurdering: Behandling? = null

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
        førstegangsbehandling = kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )

        revurdering = kjørStegprosessForRevurderingÅrligKontroll(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                vedtakService = vedtakService,
                stegService = stegService,
                tilbakekrevingService = tilbakekrevingService
        )
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
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)

        val fom = inneværendeMåned().minusMonths(12).førsteDagIInneværendeMåned()
        val tom = inneværendeMåned().sisteDagIInneværendeMåned()
        val type = Vedtaksperiodetype.FORTSATT_INNVILGET
        val vedtaksperiode = VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fom,
                tom = tom,
                type = type
        )
        vedtaksperiodeRepository.save(vedtaksperiode)

        val vedtaksperiodeMedSammePeriode = VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fom,
                tom = tom,
                type = type
        )
        val feil = assertThrows<DataIntegrityViolationException> { vedtaksperiodeRepository.save(vedtaksperiodeMedSammePeriode) }
        assertTrue(feil.message!!.contains("constraint [vedtaksperiode_fk_vedtak_id_fom_tom_type_key]"))
    }
    
    @Test
    fun `Skal validere at vedtaksperioder blir lagret ved fortsatt innvilget som resultat`() {
        assertEquals(BehandlingResultat.FORTSATT_INNVILGET, revurdering?.resultat)

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering!!.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        assertEquals(1, vedtaksperioder.size)
        assertEquals(Vedtaksperiodetype.FORTSATT_INNVILGET, vedtaksperioder.first().type)
    }

    @Test
    fun `Skal legge til og overskrive begrunnelser og fritekst på vedtaksperiode`() {
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering!!.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(
                vedtaksperiodeId = vedtaksperioder.first().id,
                restPutVedtaksperiodeMedBegrunnelse = RestPutVedtaksperiodeMedBegrunnelse(
                        begrunnelser = listOf(RestVedtaksbegrunnelse(
                                vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
                                personIdenter = listOf(søkerFnr, barnFnr)
                        )),
                        fritekster = listOf("Eksempel på fritekst for fortsatt innvilget periode")
                )
        )

        val vedtaksperioderMedUtfylteBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
        assertEquals(1, vedtaksperioderMedUtfylteBegrunnelser.size)
        assertEquals(1, vedtaksperioderMedUtfylteBegrunnelser.first().begrunnelser.size)
        assertEquals(VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
                     vedtaksperioderMedUtfylteBegrunnelser.first().begrunnelser.first().vedtakBegrunnelseSpesifikasjon)
        assertEquals(1, vedtaksperioderMedUtfylteBegrunnelser.first().fritekster.size)

        vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(
                vedtaksperiodeId = vedtaksperioder.first().id,
                restPutVedtaksperiodeMedBegrunnelse = RestPutVedtaksperiodeMedBegrunnelse(
                        begrunnelser = listOf(RestVedtaksbegrunnelse(
                                vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_FAST_OMSORG,
                                personIdenter = listOf(søkerFnr, barnFnr)
                        )),
                )
        )

        val vedtaksperioderMedOverskrevneBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
        assertEquals(1, vedtaksperioderMedOverskrevneBegrunnelser.size)
        assertEquals(1, vedtaksperioderMedOverskrevneBegrunnelser.first().begrunnelser.size)
        assertEquals(VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_FAST_OMSORG,
                     vedtaksperioderMedOverskrevneBegrunnelser.first().begrunnelser.first().vedtakBegrunnelseSpesifikasjon)
        assertEquals(0, vedtaksperioderMedOverskrevneBegrunnelser.first().fritekster.size)
    }

    @Test
    fun `Skal kaste feil når feil type blir valgt`() {
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering!!.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        val feil = assertThrows<Feil> {
            vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(
                    vedtaksperiodeId = vedtaksperioder.first().id,
                    restPutVedtaksperiodeMedBegrunnelse = RestPutVedtaksperiodeMedBegrunnelse(
                            begrunnelser = listOf(RestVedtaksbegrunnelse(
                                    vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER,
                                    personIdenter = listOf(søkerFnr, barnFnr)
                            )),
                    )
            )
        }

        assertEquals("Begrunnelsestype INNVILGELSE passer ikke med typen 'FORTSATT_INNVILGET' som er satt på perioden.",
                     feil.message)
    }

    @Test
    fun `Skal kaste feil når fritekst blir brukt feil`() {
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering!!.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        val feil = assertThrows<Feil> {
            vedtaksperiodeService.oppdaterVedtaksperiodeMedBegrunnelser(
                    vedtaksperiodeId = vedtaksperioder.first().id,
                    restPutVedtaksperiodeMedBegrunnelse = RestPutVedtaksperiodeMedBegrunnelse(
                            begrunnelser = listOf(RestVedtaksbegrunnelse(
                                    vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_FRITEKST,
                                    personIdenter = listOf(søkerFnr, barnFnr)
                            )),
                    )
            )
        }

        assertEquals("Kan ikke fastsette fritekstbegrunnelse på begrunnelser på vedtaksperioder. Bruk heller fritekster.",
                     feil.message)
    }
}