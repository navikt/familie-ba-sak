package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VedtaksperiodeServiceTest(
    @Autowired
    private val stegService: StegService,

    @Autowired
    private val vedtakService: VedtakService,

    @Autowired
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,

    @Autowired
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val beregningService: BeregningService,

    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val økonomiService: ØkonomiService,

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
            barnasIdenter = listOf(barnFnr, barn2Fnr),
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
            barnasIdenter = listOf(barnFnr, barn2Fnr),
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
    fun `Skal kunne lagre flere vedtaksperioder av typen endret utbetaling med samme periode`() {
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
        val type = Vedtaksperiodetype.ENDRET_UTBETALING
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

        Assertions.assertDoesNotThrow {
            vedtaksperiodeRepository.save(vedtaksperiodeMedSammePeriode)
        }
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

    @Test
    fun `skal lage redusert vedtaksperioder`() {
        val behandling = førstegangsbehandling!!
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandling.id)!!
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandling.id)
        val periodeFom = LocalDate.now().minusMonths(6)
        personopplysningGrunnlag.søkerOgBarn.forEach {
            vurderVilkårsvurderingTilInnvilget(vilkårsvurdering, it, periodeFom)
        }
        vilkårsvurderingService.oppdater(vilkårsvurdering)
        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
        var vedtak = vedtakService.hentAktivForBehandlingThrows(behandling.id)
        vedtaksperiodeService.lagre(vedtaksperiodeService.genererVedtaksperioderMedBegrunnelser(vedtak))
        økonomiService.oppdaterTilkjentYtelseMedUtbetalingsoppdragOgIverksett(vedtak, "1234")

        var andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)
        assertNotNull(andelTilkjentYtelser)
        assertTrue { andelTilkjentYtelser.any { it.stønadFom == periodeFom.nesteMåned() } }

        val revurdering = revurdering!!
        val revVilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(revurdering.id)!!
        val revPersonopplysningGrunnlag = persongrunnlagService.hentAktivThrows(revurdering.id)
        val nyPeriode = LocalDate.now().minusMonths(3)
        revPersonopplysningGrunnlag.søkerOgBarn.forEach {
            if (it.aktør == tilAktør(barnFnr)) {
                vurderVilkårsvurderingTilInnvilget(revVilkårsvurdering, it, nyPeriode)
            } else {
                vurderVilkårsvurderingTilInnvilget(revVilkårsvurdering, it, periodeFom)
            }
        }
        vilkårsvurderingService.oppdater(revVilkårsvurdering)
        beregningService.oppdaterBehandlingMedBeregning(revurdering, revPersonopplysningGrunnlag)
        behandlingService.oppdaterResultatPåBehandling(revurdering.id, BehandlingResultat.ENDRET)

        vedtak = vedtakService.hentAktivForBehandlingThrows(revurdering.id)
        vedtaksperiodeService.lagre(vedtaksperiodeService.genererVedtaksperioderMedBegrunnelser(vedtak))

        andelTilkjentYtelser = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(revurdering.id)
        assertNotNull(andelTilkjentYtelser)
        assertTrue { andelTilkjentYtelser.any { it.stønadFom == nyPeriode.nesteMåned() } }

        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
        assertNotNull(vedtaksperioder)
        assertTrue { vedtaksperioder.any { it.type == Vedtaksperiodetype.REDUKSJON } }
        val redusertPeriode = vedtaksperioder.single { it.type == Vedtaksperiodetype.REDUKSJON }
        assertTrue {
            redusertPeriode.fom == periodeFom.nesteMåned().førsteDagIInneværendeMåned() &&
                redusertPeriode.tom == nyPeriode.sisteDagIMåned()
        }
    }

    private fun vurderVilkårsvurderingTilInnvilget(
        vilkårsvurdering: Vilkårsvurdering,
        barn: Person,
        periodeFom: LocalDate
    ) {
        vilkårsvurdering.personResultater.filter { it.aktør == barn.aktør }.forEach { personResultat ->
            personResultat.vilkårResultater.forEach {
                if (it.vilkårType == Vilkår.UNDER_18_ÅR) {
                    it.resultat = Resultat.OPPFYLT
                    it.periodeFom = barn.fødselsdato
                    it.periodeTom = barn.fødselsdato.plusYears(18)
                } else {
                    it.resultat = Resultat.OPPFYLT
                    it.periodeFom = periodeFom
                }
            }
        }
    }
}
