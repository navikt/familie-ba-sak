package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException

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
) : AbstractSpringIntegrationTest() {

    val søkerFnr = randomFnr()
    val barnFnr = ClientMocks.barnFnr[0]
    val barn2Fnr = ClientMocks.barnFnr[1]
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
            vedtaksperiodeService = vedtaksperiodeService,
        )

        revurdering = kjørStegprosessForRevurderingÅrligKontroll(
            tilSteg = StegType.BEHANDLINGSRESULTAT,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barnFnr),
            vedtakService = vedtakService,
            stegService = stegService
        )
    }

    @Test
    fun `Skal lage og populere avslagsperiode for uregistrert barn`() {
        val søkerFnr = randomFnr()
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.REGISTRERE_SØKNAD,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(randomFnr()),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
        )

        val behandlingEtterNySøknadsregistrering = stegService.håndterSøknad(
            behandling = behandling,
            restRegistrerSøknad = RestRegistrerSøknad(
                søknad = SøknadDTO(
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    søkerMedOpplysninger = SøkerMedOpplysninger(
                        ident = søkerFnr
                    ),
                    barnaMedOpplysninger = listOf(
                        BarnMedOpplysninger(
                            ident = "",
                            erFolkeregistrert = false,
                            inkludertISøknaden = true
                        )
                    ),
                    endringAvOpplysningerBegrunnelse = ""
                ),
                bekreftEndringerViaFrontend = true
            )
        )

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandlingEtterNySøknadsregistrering.id)

        val vedtaksperioder = vedtaksperiodeService.genererVedtaksperioderMedBegrunnelser(vedtak)

        assertEquals(1, vedtaksperioder.size)
        assertEquals(1, vedtaksperioder.flatMap { it.begrunnelser }.size)
        assertEquals(
            VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN,
            vedtaksperioder.flatMap { it.begrunnelser }.first().vedtakBegrunnelseSpesifikasjon
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
            vedtaksperiodeService = vedtaksperiodeService,
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
        val feil =
            assertThrows<DataIntegrityViolationException> { vedtaksperiodeRepository.save(vedtaksperiodeMedSammePeriode) }
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

        vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperioder.first().id,
            standardbegrunnelserFraFrontend = listOf(VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_BARN_OG_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE)
        )

        val vedtaksperioderMedUtfylteBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
        assertEquals(1, vedtaksperioderMedUtfylteBegrunnelser.size)
        assertEquals(1, vedtaksperioderMedUtfylteBegrunnelser.first().begrunnelser.size)
        assertEquals(
            VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_BARN_OG_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
            vedtaksperioderMedUtfylteBegrunnelser.first().begrunnelser.first().vedtakBegrunnelseSpesifikasjon
        )

        vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperioder.first().id,
            standardbegrunnelserFraFrontend = listOf(VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_FAST_OMSORG),
        )

        val vedtaksperioderMedOverskrevneBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
        assertEquals(1, vedtaksperioderMedOverskrevneBegrunnelser.size)
        assertEquals(1, vedtaksperioderMedOverskrevneBegrunnelser.first().begrunnelser.size)
        assertEquals(
            VedtakBegrunnelseSpesifikasjon.FORTSATT_INNVILGET_FAST_OMSORG,
            vedtaksperioderMedOverskrevneBegrunnelser.first().begrunnelser.first().vedtakBegrunnelseSpesifikasjon
        )
        assertEquals(0, vedtaksperioderMedOverskrevneBegrunnelser.first().fritekster.size)
    }

    @Test
    fun `Skal kaste feil når begrunnelser som ikke samsvarer med vilkårsvurdering blir valgt`() {
        val behandling = kjørStegprosessForFGB(
            tilSteg = StegType.BEHANDLINGSRESULTAT,
            søkerFnr = randomFnr(),
            barnasIdenter = listOf(barn2Fnr),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
        )

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        val funksjonellFeil = assertThrows<FunksjonellFeil> {
            vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
                vedtaksperiodeId = vedtaksperioder.first().id,
                standardbegrunnelserFraFrontend = listOf(VedtakBegrunnelseSpesifikasjon.REDUKSJON_BOSATT_I_RIKTET)
            )
        }
        assertTrue(funksjonellFeil.frontendFeilmelding?.contains("REDUKSJON_BOSATT_I_RIKTET' forventer vurdering på 'Bosatt i riket'") == true)
    }

    @Test
    fun `Skal kaste feil når feil type blir valgt`() {
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering!!.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        val feil = assertThrows<Feil> {
            vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
                vedtaksperiodeId = vedtaksperioder.first().id,
                standardbegrunnelserFraFrontend = listOf(VedtakBegrunnelseSpesifikasjon.INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER),
            )
        }

        assertEquals(
            "Begrunnelsestype INNVILGET passer ikke med typen 'FORTSATT_INNVILGET' som er satt på perioden.",
            feil.message
        )
    }
}
