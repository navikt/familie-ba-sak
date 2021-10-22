package no.nav.familie.ba.sak.kjerne.grunnlag.søknad

import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.writeValueAsString
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles(
    "dev",
    "mock-pdl",
    "mock-infotrygd-barnetrygd",
    "mock-økonomi",
    "mock-tilbakekreving-klient",
    "mock-brev-klient"
)
class SøknadGrunnlagTest(
    @Autowired
    private val søknadGrunnlagService: SøknadGrunnlagService,

    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val stegService: StegService,

    @Autowired
    private val persongrunnlagService: PersongrunnlagService,

    @Autowired
    private val vedtakService: VedtakService,

    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,

    @Autowired
    private val beregningService: BeregningService,

    @Autowired
    private val tilbakekrevingsRepository: TilbakekrevingRepository,

    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,

    ) {

    @Test
    fun `Skal lagre ned og hente søknadsgrunnlag`() {
        val behandlingId = 1L
        val søkerIdent = randomFnr()
        val barnIdent = randomFnr()
        val søknadDTO = lagSøknadDTO(søkerIdent = søkerIdent, barnasIdenter = listOf(barnIdent))
        søknadGrunnlagService.lagreOgDeaktiverGammel(
            SøknadGrunnlag(
                behandlingId = behandlingId,
                søknad = søknadDTO.writeValueAsString()
            )
        )

        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId)
        assertNotNull(søknadGrunnlag)
        assertEquals(behandlingId, søknadGrunnlag?.behandlingId)
        assertEquals(true, søknadGrunnlag?.aktiv)
        assertEquals(søkerIdent, søknadGrunnlag?.hentSøknadDto()?.søkerMedOpplysninger?.ident)
    }

    @Test
    fun `Skal sjekke at det kun kan være et aktivt grunnlag for en behandling`() {
        val behandling = lagBehandling()
        val søkerIdent = randomFnr()
        val barnIdent = randomFnr()
        val søknadDTO = lagSøknadDTO(søkerIdent = søkerIdent, barnasIdenter = listOf(barnIdent))

        val søkerIdent2 = randomFnr()
        val barnIdent2 = randomFnr()
        val søknadDTO2 = lagSøknadDTO(søkerIdent = søkerIdent2, barnasIdenter = listOf(barnIdent2))

        søknadGrunnlagService.lagreOgDeaktiverGammel(
            SøknadGrunnlag(
                behandlingId = behandling.id,
                søknad = søknadDTO.writeValueAsString()
            )
        )

        søknadGrunnlagService.lagreOgDeaktiverGammel(
            SøknadGrunnlag(
                behandlingId = behandling.id,
                søknad = søknadDTO2.writeValueAsString()
            )
        )
        val søknadsGrunnlag = søknadGrunnlagService.hentAlle(behandling.id)
        assertEquals(2, søknadsGrunnlag.size)

        val aktivSøknadGrunnlag = søknadGrunnlagService.hentAktiv(behandling.id)
        assertNotNull(aktivSøknadGrunnlag)
    }

    @Test
    fun `Skal registrere søknad med uregistrerte barn og disse skal ikke komme med i persongrunnlaget`() {
        val søkerIdent = randomFnr()
        val folkeregistrertBarn = ClientMocks.barnFnr[0]
        val uregistrertBarn = randomFnr()
        val søknadDTO = SøknadDTO(
            underkategori = BehandlingUnderkategori.ORDINÆR,
            søkerMedOpplysninger = SøkerMedOpplysninger(
                ident = søkerIdent
            ),
            barnaMedOpplysninger = listOf(
                BarnMedOpplysninger(
                    ident = folkeregistrertBarn
                ),
                BarnMedOpplysninger(
                    ident = uregistrertBarn,
                    erFolkeregistrert = false
                )
            ),
            endringAvOpplysningerBegrunnelse = ""
        )

        fagsakService.hentEllerOpprettFagsak(PersonIdent(søkerIdent))
        val behandling = stegService.håndterNyBehandling(
            NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                søkerIdent,
                BehandlingType.FØRSTEGANGSBEHANDLING
            )
        )

        stegService.håndterSøknad(
            behandling = behandling,
            restRegistrerSøknad = RestRegistrerSøknad(
                søknad = søknadDTO,
                bekreftEndringerViaFrontend = false
            )
        )

        val persongrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)

        assertEquals(1, persongrunnlag!!.barna.size)
        assertTrue(persongrunnlag.barna.any { it.personIdent.ident == folkeregistrertBarn })
        assertTrue(persongrunnlag.barna.none { it.personIdent.ident == uregistrertBarn })
    }

    @Test
    fun `Skal tilbakestille behandling ved endring på søknadsregistrering`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = ClientMocks.barnFnr[0]
        val barn2Fnr = ClientMocks.barnFnr[1]
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
            tilSteg = StegType.VILKÅRSVURDERING,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barn1Fnr),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
        )

        val tilkjentYtelse =
            beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)
        val steg = behandlingEtterVilkårsvurderingSteg.behandlingStegTilstand.map { it.behandlingSteg }.toSet()
        assertEquals(
            setOf(
                StegType.REGISTRERE_SØKNAD,
                StegType.REGISTRERE_PERSONGRUNNLAG,
                StegType.VILKÅRSVURDERING,
                StegType.BEHANDLINGSRESULTAT
            ),
            steg
        )
        assertNotNull(tilkjentYtelse)
        assertTrue(tilkjentYtelse.andelerTilkjentYtelseTilUtbetaling.size > 0)

        val behandlingEtterNyRegistrering = stegService.håndterSøknad(
            behandling = behandlingEtterVilkårsvurderingSteg,
            restRegistrerSøknad = RestRegistrerSøknad(
                søknad = SøknadDTO(
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    søkerMedOpplysninger = SøkerMedOpplysninger(
                        ident = søkerFnr
                    ),
                    barnaMedOpplysninger = listOf(
                        BarnMedOpplysninger(
                            ident = barn1Fnr,
                            inkludertISøknaden = false
                        ),
                        BarnMedOpplysninger(
                            ident = barn2Fnr,
                            inkludertISøknaden = true
                        )
                    ),
                    endringAvOpplysningerBegrunnelse = ""
                ),
                bekreftEndringerViaFrontend = true
            )
        )

        val error =
            assertThrows<IllegalStateException> { beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingEtterNyRegistrering.id) }
        val stegEtterNyRegistrering =
            behandlingEtterNyRegistrering.behandlingStegTilstand.map { it.behandlingSteg }.toSet()
        assertEquals("Fant ikke tilkjent ytelse for behandling ${behandlingEtterNyRegistrering.id}", error.message)
        assertEquals(
            setOf(StegType.REGISTRERE_SØKNAD, StegType.REGISTRERE_PERSONGRUNNLAG, StegType.VILKÅRSVURDERING),
            stegEtterNyRegistrering
        )
    }

    @Test
    fun `Skal fjerne barn og mapping til restbehandling skal kjøre ok`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = ClientMocks.barnFnr[0]
        val barn2Fnr = ClientMocks.barnFnr[1]
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
            tilSteg = StegType.VILKÅRSVURDERING,
            søkerFnr = søkerFnr,
            barnasIdenter = listOf(barn1Fnr),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            persongrunnlagService = persongrunnlagService,
            vilkårsvurderingService = vilkårsvurderingService,
            stegService = stegService,
            vedtaksperiodeService = vedtaksperiodeService,
        )

        val behandlingEtterNyRegistrering = stegService.håndterSøknad(
            behandling = behandlingEtterVilkårsvurderingSteg,
            restRegistrerSøknad = RestRegistrerSøknad(
                søknad = SøknadDTO(
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    søkerMedOpplysninger = SøkerMedOpplysninger(
                        ident = søkerFnr
                    ),
                    barnaMedOpplysninger = listOf(
                        BarnMedOpplysninger(
                            ident = barn1Fnr,
                            inkludertISøknaden = false
                        ),
                        BarnMedOpplysninger(
                            ident = barn2Fnr,
                            inkludertISøknaden = true
                        )
                    ),
                    endringAvOpplysningerBegrunnelse = ""
                ),
                bekreftEndringerViaFrontend = true
            )
        )

        assertDoesNotThrow { fagsakService.lagRestUtvidetBehandling(behandling = behandlingEtterNyRegistrering) }
    }
}
