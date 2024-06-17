package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.kjørStegprosessForRevurderingÅrligKontroll
import no.nav.familie.ba.sak.common.randomBarnFnr
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.MockPersonopplysningerService
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.BehandlingUnderkategoriDTO
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class VedtaksperiodeServiceIntegrationTest(
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
    private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,
    @Autowired
    private val brevmalService: BrevmalService,
) : AbstractSpringIntegrationTest() {
    val søkerFnr = MockPersonopplysningerService.leggTilPersonInfo(personIdent = randomFnr())
    val barnFnr = MockPersonopplysningerService.leggTilPersonInfo(personIdent = randomBarnFnr(alder = 6))
    val barn2Fnr = MockPersonopplysningerService.leggTilPersonInfo(personIdent = randomBarnFnr(alder = 2))

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    private fun kjørFørstegangsbehandlingOgRevurderingÅrligKontroll(): Behandling {
        val førstegangsbehandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.BEHANDLING_AVSLUTTET,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr, barn2Fnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        return kjørStegprosessForRevurderingÅrligKontroll(
            tilSteg = StegType.BEHANDLINGSRESULTAT,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barnFnr, barn2Fnr),
            vedtakService = vedtakService,
            stegService = stegService,
            fagsakId = førstegangsbehandling.fagsak.id,
            brevmalService = brevmalService,
        )
    }

    @Test
    fun `Skal lage og populere avslagsperiode for uregistrert barn`() {
        val søkerFnr = randomFnr()
        val behandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.REGISTRERE_SØKNAD,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        val behandlingEtterNySøknadsregistrering =
            stegService.håndterSøknad(
                behandling = behandling,
                restRegistrerSøknad =
                    RestRegistrerSøknad(
                        søknad =
                            SøknadDTO(
                                underkategori = BehandlingUnderkategoriDTO.ORDINÆR,
                                søkerMedOpplysninger =
                                    SøkerMedOpplysninger(
                                        ident = søkerFnr,
                                    ),
                                barnaMedOpplysninger =
                                    listOf(
                                        BarnMedOpplysninger(
                                            ident = "",
                                            erFolkeregistrert = false,
                                            inkludertISøknaden = true,
                                        ),
                                    ),
                                endringAvOpplysningerBegrunnelse = "",
                            ),
                        bekreftEndringerViaFrontend = true,
                    ),
            )

        val vedtaksperioder =
            vedtaksperiodeService.finnVedtaksperioderForBehandling(behandlingEtterNySøknadsregistrering.id)

        assertEquals(1, vedtaksperioder.size)
        assertEquals(1, vedtaksperioder.flatMap { it.begrunnelser }.size)
        assertEquals(
            Standardbegrunnelse.AVSLAG_UREGISTRERT_BARN,
            vedtaksperioder.flatMap { it.begrunnelser }.first().standardbegrunnelse,
        )
    }

    @Test
    fun `Skal lage og populere avslagsperiode for uregistrert barn med eøs begrunnelse dersom behandling sin kategori er EØS`() {
        val søkerFnr = randomFnr()
        val behandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.REGISTRERE_SØKNAD,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
                behandlingKategori = BehandlingKategori.EØS,
            )

        val behandlingEtterNySøknadsregistrering =
            stegService.håndterSøknad(
                behandling = behandling,
                restRegistrerSøknad =
                    RestRegistrerSøknad(
                        søknad =
                            SøknadDTO(
                                underkategori = BehandlingUnderkategoriDTO.ORDINÆR,
                                søkerMedOpplysninger =
                                    SøkerMedOpplysninger(
                                        ident = søkerFnr,
                                    ),
                                barnaMedOpplysninger =
                                    listOf(
                                        BarnMedOpplysninger(
                                            ident = "",
                                            erFolkeregistrert = false,
                                            inkludertISøknaden = true,
                                        ),
                                    ),
                                endringAvOpplysningerBegrunnelse = "",
                            ),
                        bekreftEndringerViaFrontend = true,
                    ),
            )

        val vedtaksperioder =
            vedtaksperiodeService.finnVedtaksperioderForBehandling(behandlingEtterNySøknadsregistrering.id)

        assertEquals(1, vedtaksperioder.size)
        assertEquals(1, vedtaksperioder.flatMap { it.eøsBegrunnelser }.size)
        assertEquals(
            EØSStandardbegrunnelse.AVSLAG_EØS_UREGISTRERT_BARN,
            vedtaksperioder.flatMap { it.eøsBegrunnelser }.first().begrunnelse,
        )
    }

    @Test
    fun `Skal kunne lagre flere vedtaksperioder av typen endret utbetaling med samme periode`() {
        val behandling =
            kjørStegprosessForFGB(
                tilSteg = StegType.REGISTRERE_SØKNAD,
                søkerFnr = randomFnr(),
                barnasIdenter = listOf(barnFnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = behandling.id)

        val fom = inneværendeMåned().minusMonths(12).førsteDagIInneværendeMåned()
        val tom = inneværendeMåned().sisteDagIInneværendeMåned()
        val type = Vedtaksperiodetype.UTBETALING
        val vedtaksperiode =
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fom,
                tom = tom,
                type = type,
            )
        vedtaksperiodeRepository.save(vedtaksperiode)

        val vedtaksperiodeMedSammePeriode =
            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fom,
                tom = tom,
                type = type,
            )

        Assertions.assertDoesNotThrow {
            vedtaksperiodeRepository.save(vedtaksperiodeMedSammePeriode)
        }
    }

    @Test
    fun `Skal validere at vedtaksperioder blir lagret ved fortsatt innvilget som resultat`() {
        val revurdering = kjørFørstegangsbehandlingOgRevurderingÅrligKontroll()
        assertEquals(Behandlingsresultat.FORTSATT_INNVILGET, revurdering.resultat)

        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        assertEquals(1, vedtaksperioder.size)
        assertEquals(Vedtaksperiodetype.FORTSATT_INNVILGET, vedtaksperioder.first().type)
    }

    @Test
    fun `Skal legge til og overskrive begrunnelser og fritekst på vedtaksperiode`() {
        val revurdering = kjørFørstegangsbehandlingOgRevurderingÅrligKontroll()
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperioder.first().id,
            standardbegrunnelserFraFrontend = listOf(Standardbegrunnelse.FORTSATT_INNVILGET_BARN_OG_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE),
            eøsStandardbegrunnelserFraFrontend = emptyList(),
        )

        val vedtaksperioderMedUtfylteBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
        assertEquals(1, vedtaksperioderMedUtfylteBegrunnelser.size)
        assertEquals(1, vedtaksperioderMedUtfylteBegrunnelser.first().begrunnelser.size)
        assertEquals(
            Standardbegrunnelse.FORTSATT_INNVILGET_BARN_OG_SØKER_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
            vedtaksperioderMedUtfylteBegrunnelser
                .first()
                .begrunnelser
                .first()
                .standardbegrunnelse,
        )

        vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperioder.first().id,
            standardbegrunnelserFraFrontend = listOf(Standardbegrunnelse.FORTSATT_INNVILGET_FAST_OMSORG),
            eøsStandardbegrunnelserFraFrontend = emptyList(),
        )

        val vedtaksperioderMedOverskrevneBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)
        assertEquals(1, vedtaksperioderMedOverskrevneBegrunnelser.size)
        assertEquals(1, vedtaksperioderMedOverskrevneBegrunnelser.first().begrunnelser.size)
        assertEquals(
            Standardbegrunnelse.FORTSATT_INNVILGET_FAST_OMSORG,
            vedtaksperioderMedOverskrevneBegrunnelser
                .first()
                .begrunnelser
                .first()
                .standardbegrunnelse,
        )
        assertEquals(0, vedtaksperioderMedOverskrevneBegrunnelser.first().fritekster.size)
    }

    @Test
    fun `Skal kaste feil når feil type blir valgt`() {
        val revurdering = kjørFørstegangsbehandlingOgRevurderingÅrligKontroll()
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = revurdering.id)
        val vedtaksperioder = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak)

        val feil =
            assertThrows<Feil> {
                vedtaksperiodeService.oppdaterVedtaksperiodeMedStandardbegrunnelser(
                    vedtaksperiodeId = vedtaksperioder.first().id,
                    standardbegrunnelserFraFrontend = listOf(Standardbegrunnelse.INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER),
                    eøsStandardbegrunnelserFraFrontend = emptyList(),
                )
            }

        assertEquals(
            "Begrunnelsestype INNVILGET passer ikke med typen 'FORTSATT_INNVILGET' som er satt på perioden.",
            feil.message,
        )
    }
}
