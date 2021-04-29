package no.nav.familie.ba.sak.behandling.søknad

import no.nav.familie.ba.sak.behandling.NyBehandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlag
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.behandling.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.behandling.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.restDomene.writeValueAsString
import no.nav.familie.ba.sak.behandling.steg.StegService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.kjørStegprosessForFGB
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("dev", "mock-pdl", "mock-simulering")
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
        private val beregningService: BeregningService
) {

    @Test
    fun `Skal lagre ned og hente søknadsgrunnlag`() {
        val behandlingId = 1L
        val søkerIdent = randomFnr()
        val barnIdent = randomFnr()
        val søknadDTO = lagSøknadDTO(søkerIdent = søkerIdent, barnasIdenter = listOf(barnIdent))
        søknadGrunnlagService.lagreOgDeaktiverGammel(SøknadGrunnlag(
                behandlingId = behandlingId,
                søknad = søknadDTO.writeValueAsString()
        ))

        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId)
        assertNotNull(søknadGrunnlag)
        assertEquals(behandlingId, søknadGrunnlag?.behandlingId)
        assertEquals(true, søknadGrunnlag?.aktiv)
        assertEquals(søkerIdent, søknadGrunnlag?.hentSøknadDto()?.søkerMedOpplysninger?.ident)
    }

    @Test
    fun `Skal sjekke at det kun kan være et aktivt grunnlag for en behandling`() {
        val behandlingId = 2L
        val søkerIdent = randomFnr()
        val barnIdent = randomFnr()
        val søknadDTO = lagSøknadDTO(søkerIdent = søkerIdent, barnasIdenter = listOf(barnIdent))

        val søkerIdent2 = randomFnr()
        val barnIdent2 = randomFnr()
        val søknadDTO2 = lagSøknadDTO(søkerIdent = søkerIdent2, barnasIdenter = listOf(barnIdent2))

        søknadGrunnlagService.lagreOgDeaktiverGammel(SøknadGrunnlag(
                behandlingId = behandlingId,
                søknad = søknadDTO.writeValueAsString()
        ))

        søknadGrunnlagService.lagreOgDeaktiverGammel(SøknadGrunnlag(
                behandlingId = behandlingId,
                søknad = søknadDTO2.writeValueAsString()
        ))
        val søknadsGrunnlag = søknadGrunnlagService.hentAlle(behandlingId)
        assertEquals(2, søknadsGrunnlag.size)

        val aktivSøknadGrunnlag = søknadGrunnlagService.hentAktiv(behandlingId)
        assertNotNull(aktivSøknadGrunnlag)
    }

    @Test
    fun `Skal registrere søknad med uregistrerte barn og disse skal ikke komme med i persongrunnlaget`() {
        val søkerIdent = ClientMocks.søkerFnr[0]
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
        val behandling = stegService.håndterNyBehandling(NyBehandling(
                BehandlingKategori.NASJONAL,
                BehandlingUnderkategori.ORDINÆR,
                søkerIdent,
                BehandlingType.FØRSTEGANGSBEHANDLING
        ))

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
    fun `Skal slette tilkjent ytelse ved endring på søknadsregistrering`() {
        val behandlingEtterVilkårsvurderingSteg = kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = ClientMocks.søkerFnr[0],
                barnasIdenter = listOf(ClientMocks.barnFnr[0]),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService
        )

        val tilkjentYtelse =
                beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)
        assertNotNull(tilkjentYtelse)
        assert(tilkjentYtelse.andelerTilkjentYtelse.size > 0)

        val behandlingEtterNyRegistrering = stegService.håndterSøknad(behandling = behandlingEtterVilkårsvurderingSteg,
                                  restRegistrerSøknad = RestRegistrerSøknad(
                                          søknad = SøknadDTO(
                                                  underkategori = BehandlingUnderkategori.ORDINÆR,
                                                  søkerMedOpplysninger = SøkerMedOpplysninger(
                                                          ident = ClientMocks.søkerFnr[0]
                                                  ),
                                                  barnaMedOpplysninger = listOf(
                                                          BarnMedOpplysninger(
                                                                  ident = ClientMocks.barnFnr[0],
                                                                  inkludertISøknaden = false
                                                          ),
                                                          BarnMedOpplysninger(
                                                                  ident = ClientMocks.barnFnr[1],
                                                                  inkludertISøknaden = true
                                                          )
                                                  ),
                                                  endringAvOpplysningerBegrunnelse = ""
                                          ),
                                          bekreftEndringerViaFrontend = true))

        val error = assertThrows<IllegalStateException> { beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingEtterNyRegistrering.id) }
        assertEquals("Fant ikke tilkjent ytelse for behandling ${behandlingEtterNyRegistrering.id}", error.message)
    }
}