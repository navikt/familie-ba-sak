package no.nav.familie.ba.sak.kjerne.grunnlag.søknad

import io.mockk.every
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.MockPersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.datagenerator.randomBarnFnr
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.BehandlingUnderkategoriDTO
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.SøkerMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.writeValueAsString
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.Søknadsinfo
import no.nav.familie.ba.sak.kjerne.behandling.UtvidetBehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjørbehandling.kjørStegprosessForFGB
import no.nav.familie.kontrakter.ba.søknad.VersjonertBarnetrygdSøknadV9
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import no.nav.familie.kontrakter.ba.søknad.v8.Barn
import no.nav.familie.kontrakter.ba.søknad.v8.Søker
import no.nav.familie.kontrakter.ba.søknad.v9.BarnetrygdSøknad
import no.nav.familie.kontrakter.felles.søknad.Søknadsfelt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import java.time.LocalDate

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
    private val personidentService: PersonidentService,
    @Autowired
    private val vedtakService: VedtakService,
    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,
    @Autowired
    private val beregningService: BeregningService,
    @Autowired
    private val utvidetBehandlingService: UtvidetBehandlingService,
    @Autowired
    private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,
    @Autowired
    private val brevmalService: BrevmalService,
    @Autowired
    private val integrasjonClient: IntegrasjonClient,
) : AbstractSpringIntegrationTest() {
    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal lagre ned og hente søknadsgrunnlag`() {
        val søkerIdent = randomFnr()
        val barnIdent = randomFnr()
        val søkerAktør = personidentService.hentAktør(søkerIdent)

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerAktør.aktivFødselsnummer())
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(søkerIdent, fagsak.id),
            )

        val søknadDTO = lagSøknadDTO(søkerIdent = søkerIdent, barnasIdenter = listOf(barnIdent))
        søknadGrunnlagService.lagreOgDeaktiverGammel(
            SøknadGrunnlag(
                behandlingId = behandling.id,
                søknad = søknadDTO.writeValueAsString(),
            ),
        )

        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandling.id)
        assertNotNull(søknadGrunnlag)
        assertEquals(behandling.id, søknadGrunnlag?.behandlingId)
        assertEquals(true, søknadGrunnlag?.aktiv)
        assertEquals(søkerIdent, søknadGrunnlag?.hentSøknadDto()?.søkerMedOpplysninger?.ident)
    }

    @Test
    fun `Skal sjekke at det kun kan være et aktivt grunnlag for en behandling`() {
        val søkerIdent = randomFnr()
        val barnIdent = randomFnr()
        val søkerAktør = personidentService.hentAktør(søkerIdent)

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerAktør.aktivFødselsnummer())
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(søkerIdent, fagsak.id),
            )
        val søknadDTO = lagSøknadDTO(søkerIdent = søkerIdent, barnasIdenter = listOf(barnIdent))

        val barnIdent2 = randomFnr()
        val søknadDTO2 = lagSøknadDTO(søkerIdent = søkerIdent, barnasIdenter = listOf(barnIdent2))

        søknadGrunnlagService.lagreOgDeaktiverGammel(
            SøknadGrunnlag(
                behandlingId = behandling.id,
                søknad = søknadDTO.writeValueAsString(),
            ),
        )

        søknadGrunnlagService.lagreOgDeaktiverGammel(
            SøknadGrunnlag(
                behandlingId = behandling.id,
                søknad = søknadDTO2.writeValueAsString(),
            ),
        )
        val søknadsGrunnlag = søknadGrunnlagService.hentAlle(behandling.id)
        assertEquals(2, søknadsGrunnlag.size)

        val aktivSøknadGrunnlag = søknadGrunnlagService.hentAktiv(behandling.id)
        assertNotNull(aktivSøknadGrunnlag)
    }

    @Test
    fun `Skal registrere søknad med uregistrerte barn og disse skal ikke komme med i persongrunnlaget`() {
        val søkerIdent = randomFnr()
        val folkeregistrertBarn = randomFnr()
        val uregistrertBarn = randomFnr()

        val søkerAktør = personidentService.hentAktør(søkerIdent)

        val søknadDTO =
            SøknadDTO(
                underkategori = BehandlingUnderkategoriDTO.ORDINÆR,
                søkerMedOpplysninger =
                    SøkerMedOpplysninger(
                        ident = søkerIdent,
                    ),
                barnaMedOpplysninger =
                    listOf(
                        BarnMedOpplysninger(
                            ident = folkeregistrertBarn,
                        ),
                        BarnMedOpplysninger(
                            ident = uregistrertBarn,
                            erFolkeregistrert = false,
                        ),
                    ),
                endringAvOpplysningerBegrunnelse = "",
            )

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerAktør.aktivFødselsnummer())
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(søkerIdent, fagsak.id),
            )

        stegService.håndterSøknad(
            behandling = behandling,
            restRegistrerSøknad =
                RestRegistrerSøknad(
                    søknad = søknadDTO,
                    bekreftEndringerViaFrontend = false,
                ),
        )

        val persongrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)

        assertEquals(1, persongrunnlag!!.barna.size)
        assertTrue(persongrunnlag.barna.any { it.aktør.aktivFødselsnummer() == folkeregistrertBarn })
        assertTrue(persongrunnlag.barna.none { it.aktør.aktivFødselsnummer() == uregistrertBarn })
    }

    private fun lagNyBehandling(
        søkerIdent: String,
        fagsakId: Long,
        journalpostIdSøknad: String? = null,
    ) = NyBehandling(
        kategori = BehandlingKategori.NASJONAL,
        underkategori = BehandlingUnderkategori.ORDINÆR,
        søkersIdent = søkerIdent,
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        søknadMottattDato = LocalDate.now(),
        fagsakId = fagsakId,
        søknadsinfo =
            journalpostIdSøknad?.let {
                Søknadsinfo(
                    journalpostId = it,
                    brevkode = null,
                    erDigital = true,
                )
            },
    )

    @Test
    fun `Skal tilbakestille behandling ved endring på søknadsregistrering`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = leggTilPersonInfo(randomBarnFnr(alder = 6))
        val barn2Fnr = leggTilPersonInfo(randomBarnFnr(alder = 2))
        val behandlingEtterVilkårsvurderingSteg =
            kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barn1Fnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        val tilkjentYtelse =
            beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingEtterVilkårsvurderingSteg.id)
        val steg = behandlingEtterVilkårsvurderingSteg.behandlingStegTilstand.map { it.behandlingSteg }.toSet()
        assertEquals(
            setOf(
                StegType.REGISTRERE_SØKNAD,
                StegType.REGISTRERE_PERSONGRUNNLAG,
                StegType.VILKÅRSVURDERING,
                StegType.BEHANDLINGSRESULTAT,
            ),
            steg,
        )
        assertNotNull(tilkjentYtelse)
        assertTrue(tilkjentYtelse.andelerTilkjentYtelse.size > 0)

        val behandlingEtterNyRegistrering =
            stegService.håndterSøknad(
                behandling = behandlingEtterVilkårsvurderingSteg,
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
                                            ident = barn1Fnr,
                                            inkludertISøknaden = false,
                                        ),
                                        BarnMedOpplysninger(
                                            ident = barn2Fnr,
                                            inkludertISøknaden = true,
                                        ),
                                    ),
                                endringAvOpplysningerBegrunnelse = "",
                            ),
                        bekreftEndringerViaFrontend = true,
                    ),
            )

        assertThrows<EmptyResultDataAccessException> { beregningService.hentTilkjentYtelseForBehandling(behandlingId = behandlingEtterNyRegistrering.id) }
        val stegEtterNyRegistrering =
            behandlingEtterNyRegistrering.behandlingStegTilstand.map { it.behandlingSteg }.toSet()
        assertEquals(
            setOf(StegType.REGISTRERE_SØKNAD, StegType.REGISTRERE_PERSONGRUNNLAG, StegType.VILKÅRSVURDERING),
            stegEtterNyRegistrering,
        )
    }

    @Test
    fun `Skal fjerne barn og mapping til restbehandling skal kjøre ok`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()
        val behandlingEtterVilkårsvurderingSteg =
            kjørStegprosessForFGB(
                tilSteg = StegType.VILKÅRSVURDERING,
                søkerFnr = søkerFnr,
                barnasIdenter = listOf(barn1Fnr),
                fagsakService = fagsakService,
                vedtakService = vedtakService,
                persongrunnlagService = persongrunnlagService,
                vilkårsvurderingService = vilkårsvurderingService,
                stegService = stegService,
                vedtaksperiodeService = vedtaksperiodeService,
                brevmalService = brevmalService,
            )

        val behandlingEtterNyRegistrering =
            stegService.håndterSøknad(
                behandling = behandlingEtterVilkårsvurderingSteg,
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
                                            ident = barn1Fnr,
                                            inkludertISøknaden = false,
                                        ),
                                        BarnMedOpplysninger(
                                            ident = barn2Fnr,
                                            inkludertISøknaden = true,
                                        ),
                                    ),
                                endringAvOpplysningerBegrunnelse = "",
                            ),
                        bekreftEndringerViaFrontend = true,
                    ),
            )

        assertDoesNotThrow { utvidetBehandlingService.lagRestUtvidetBehandling(behandlingId = behandlingEtterNyRegistrering.id) }
    }

    @Test
    fun `Skal automatisk registrere søknad`() {
        // Arrange
        val søkerIdent = randomFnr()
        val barnIdent = randomFnr()
        val søkerAktør = personidentService.hentAktør(søkerIdent)

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerAktør.aktivFødselsnummer())

        val journalpostIdSøknad = "123456789"

        every { integrasjonClient.hentVersjonertBarnetrygdSøknad(journalpostIdSøknad) } returns
            lagVersjonertBarnetrygdSøknadV9(
                søkerIdent = søkerIdent,
                barnasIdenter = listOf(barnIdent),
                søknadstype = Søknadstype.UTVIDET,
                originalspråk = "nn",
            )

        // Act
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(
                    søkerIdent = søkerIdent,
                    fagsakId = fagsak.id,
                    journalpostIdSøknad = journalpostIdSøknad,
                ),
            )

        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandling.id)
        assertThat(søknadGrunnlag).isNotNull()

        val søknadDto = søknadGrunnlag!!.hentSøknadDto()
        assertThat(søknadDto.underkategori).isEqualTo(BehandlingUnderkategoriDTO.UTVIDET)

        assertThat(søknadDto.søkerMedOpplysninger.ident).isEqualTo(søkerIdent)
        assertThat(søknadDto.søkerMedOpplysninger.målform).isEqualTo(Målform.NN)

        assertThat(søknadDto.barnaMedOpplysninger[0].ident).isEqualTo(barnIdent)
        assertThat(søknadDto.barnaMedOpplysninger[0].inkludertISøknaden).isTrue()
        assertThat(søknadDto.barnaMedOpplysninger[0].erFolkeregistrert).isTrue()
        assertThat(søknadDto.barnaMedOpplysninger[0].manueltRegistrert).isFalse()

        val persongrunnlag = persongrunnlagService.hentAktiv(behandling.id)
        assertThat(persongrunnlag).isNotNull()
        assertThat(persongrunnlag!!.søker.aktør.aktivFødselsnummer()).isEqualTo(søkerIdent)
        assertThat(persongrunnlag.barna).hasSize(1)
        assertThat(persongrunnlag.barna[0].aktør.aktivFødselsnummer()).isEqualTo(barnIdent)
    }

    @Test
    fun `Skal returnere behandling etter registrere persongrunnlag hvis behandling ikke har søknad`() {
        // Arrange
        val søkerIdent = randomFnr()
        val barnIdent = randomFnr()
        val søkerAktør = personidentService.hentAktør(søkerIdent)

        val fagsak = fagsakService.hentEllerOpprettFagsak(søkerAktør.aktivFødselsnummer())

        // Act
        val behandling =
            stegService.håndterNyBehandling(
                lagNyBehandling(
                    søkerIdent = søkerIdent,
                    fagsakId = fagsak.id,
                    // Sender ikke med journalpostIdSøknad
                ),
            )

        assertThat(behandling.steg).isEqualTo(StegType.REGISTRERE_SØKNAD)

        val søknadGrunnlag = søknadGrunnlagService.hentAktiv(behandling.id)
        assertThat(søknadGrunnlag).isNull()

        val persongrunnlag = persongrunnlagService.hentAktiv(behandling.id)
        assertThat(persongrunnlag).isNotNull()
        assertThat(persongrunnlag!!.søker.aktør.aktivFødselsnummer()).isEqualTo(søkerIdent)
        assertThat(persongrunnlag.barna).isEmpty()
    }

    private fun lagVersjonertBarnetrygdSøknadV9(
        søkerIdent: String,
        barnasIdenter: List<String>,
        søknadstype: Søknadstype = Søknadstype.ORDINÆR,
        originalspråk: String = "nb",
    ): VersjonertBarnetrygdSøknadV9 {
        fun <T> tomtSøknadsfelt() =
            Søknadsfelt<T>(
                label = emptyMap(),
                verdi = emptyMap(),
            )
        return VersjonertBarnetrygdSøknadV9(
            barnetrygdSøknad =
                BarnetrygdSøknad(
                    kontraktVersjon = 9,
                    søker =
                        Søker(
                            ident =
                                Søknadsfelt(
                                    label = emptyMap(),
                                    verdi = mapOf("nb" to søkerIdent),
                                ),
                            harEøsSteg = false,
                            navn = tomtSøknadsfelt(),
                            statsborgerskap = tomtSøknadsfelt(),
                            adresse = tomtSøknadsfelt(),
                            adressebeskyttelse = false,
                            sivilstand = tomtSøknadsfelt(),
                            spørsmål = emptyMap(),
                        ),
                    barn =
                        barnasIdenter.map {
                            Barn(
                                ident =
                                    Søknadsfelt(
                                        label = emptyMap(),
                                        verdi = mapOf("nb" to it),
                                    ),
                                harEøsSteg = false,
                                navn = tomtSøknadsfelt(),
                                registrertBostedType = tomtSøknadsfelt(),
                                spørsmål = emptyMap(),
                            )
                        },
                    antallEøsSteg = 0,
                    søknadstype = søknadstype,
                    finnesPersonMedAdressebeskyttelse = false,
                    spørsmål = emptyMap(),
                    dokumentasjon = emptyList(),
                    teksterUtenomSpørsmål = emptyMap(),
                    originalSpråk = originalspråk,
                ),
        )
    }
}
