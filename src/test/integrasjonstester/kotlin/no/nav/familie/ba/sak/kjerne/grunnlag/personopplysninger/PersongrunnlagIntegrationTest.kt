package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.nyOrdinærBehandling
import no.nav.familie.ba.sak.datagenerator.randomBarnFødselsdato
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.DødsfallData
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlKontaktinformasjonForDødsbo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlKontaktinformasjonForDødsboAdresse
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.defaultBostedsadresseHistorikk
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class PersongrunnlagIntegrationTest(
    @Autowired
    private val persongrunnlagService: PersongrunnlagService,
    @Autowired
    private val personidentService: PersonidentService,
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val behandlingService: BehandlingService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal lagre dødsfall på person når person er død`() {
        val dødsdato = "2020-04-04"
        val adresselinje1 = "Gatenavn 1"
        val poststedsnavn = "Oslo"
        val postnummer = "1234"

        val søker1Fnr =
            leggTilPersonInfo(
                randomSøkerFødselsdato(),
                PersonInfo(
                    fødselsdato = LocalDate.of(1990, 1, 1),
                    dødsfall = DødsfallData(erDød = true, dødsdato = dødsdato),
                    kontaktinformasjonForDoedsbo =
                        PdlKontaktinformasjonForDødsbo(
                            adresse =
                                PdlKontaktinformasjonForDødsboAdresse(
                                    adresselinje1 = adresselinje1,
                                    poststedsnavn = poststedsnavn,
                                    postnummer = postnummer,
                                ),
                        ),
                ),
            )

        val barn1Fnr =
            leggTilPersonInfo(
                randomBarnFødselsdato(),
                PersonInfo(fødselsdato = LocalDate.of(2009, 1, 1)),
            )

        val søkerAktør = personidentService.hentOgLagreAktør(søker1Fnr, true)
        val barn1Aktør = personidentService.hentOgLagreAktør(barn1Fnr, true)

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerAktør.aktivFødselsnummer()))
        val behandling =
            behandlingService.opprettBehandling(
                nyOrdinærBehandling(
                    søkersIdent = søkerAktør.aktivFødselsnummer(),
                    fagsakId = fagsak.data!!.id,
                ),
            )

        val personopplysningGrunnlag =
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = søkerAktør,
                barnFraInneværendeBehandling = listOf(barn1Aktør),
                behandling = behandling,
                målform = Målform.NB,
            )

        Assertions.assertTrue(personopplysningGrunnlag.søker.erDød())
        assertEquals(LocalDate.parse(dødsdato), personopplysningGrunnlag.søker.dødsfall?.dødsfallDato)
        assertEquals(adresselinje1, personopplysningGrunnlag.søker.dødsfall?.dødsfallAdresse)
        assertEquals(postnummer, personopplysningGrunnlag.søker.dødsfall?.dødsfallPostnummer)
        assertEquals(poststedsnavn, personopplysningGrunnlag.søker.dødsfall?.dødsfallPoststed)

        Assertions.assertFalse(personopplysningGrunnlag.barna.single().erDød())
        assertEquals(null, personopplysningGrunnlag.barna.single().dødsfall)
    }

    @Test
    fun `Skal hente arbeidsforhold for mor når hun er EØS-borger og det er en automatisk behandling`() {
        val fødselsnrMor =
            leggTilPersonInfo(
                randomSøkerFødselsdato(),
                PersonInfo(
                    fødselsdato = LocalDate.of(1990, 1, 1),
                    statsborgerskap =
                        listOf(
                            Statsborgerskap(
                                land = "POL",
                                gyldigFraOgMed = null,
                                gyldigTilOgMed = null,
                                bekreftelsesdato = null,
                            ),
                        ),
                ),
            )
        val morAktør = personidentService.hentOgLagreAktør(fødselsnrMor, true)

        val barn1Fnr =
            leggTilPersonInfo(
                randomBarnFødselsdato(),
                PersonInfo(fødselsdato = LocalDate.of(2009, 1, 1)),
            )
        val barn1Aktør = personidentService.hentOgLagreAktør(barn1Fnr, true)

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = morAktør.aktivFødselsnummer()))
        val behandling =
            behandlingService.opprettBehandling(
                NyBehandling(
                    skalBehandlesAutomatisk = true,
                    søkersIdent = morAktør.aktivFødselsnummer(),
                    behandlingÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    // alltid NASJONAL for fødselshendelse
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    fagsakId = fagsak.data!!.id,
                ),
            )

        val personopplysningGrunnlag =
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = morAktør,
                barnFraInneværendeBehandling = listOf(barn1Aktør),
                behandling = behandling,
                målform = Målform.NB,
            )

        val søker = personopplysningGrunnlag.personer.single { it.type == PersonType.SØKER }

        assertThat(søker.arbeidsforhold).isNotEmpty()
    }

    @Test
    fun `Skal ikke hente arbeidsforhold for mor når det er en automatisk behandling, men hun er norsk statsborger`() {
        val fødselsnrMor =
            leggTilPersonInfo(
                randomSøkerFødselsdato(),
                PersonInfo(
                    fødselsdato = LocalDate.of(1990, 1, 1),
                    statsborgerskap =
                        listOf(
                            Statsborgerskap(
                                land = "NOR",
                                gyldigFraOgMed = null,
                                gyldigTilOgMed = null,
                                bekreftelsesdato = null,
                            ),
                        ),
                ),
            )
        val morAktør = personidentService.hentOgLagreAktør(fødselsnrMor, true)

        val fødselsnrBarn =
            leggTilPersonInfo(
                randomBarnFødselsdato(),
                PersonInfo(fødselsdato = LocalDate.of(2009, 1, 1)),
            )
        val barn1Aktør = personidentService.hentOgLagreAktør(fødselsnrBarn, true)

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = morAktør.aktivFødselsnummer()))
        val behandling =
            behandlingService.opprettBehandling(
                NyBehandling(
                    skalBehandlesAutomatisk = true,
                    søkersIdent = morAktør.aktivFødselsnummer(),
                    behandlingÅrsak = BehandlingÅrsak.FØDSELSHENDELSE,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    fagsakId = fagsak.data!!.id,
                ),
            )

        val personopplysningGrunnlag =
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                aktør = morAktør,
                barnFraInneværendeBehandling = listOf(barn1Aktør),
                behandling = behandling,
                målform = Målform.NB,
            )

        val søker = personopplysningGrunnlag.personer.single { it.type == PersonType.SØKER }

        assertThat(søker.arbeidsforhold).isEmpty()
    }

    @Test
    fun `Skal filtrere ut bostedsadresse uten verdier når de mappes inn`() {
        val fødselsdatoSøker = LocalDate.of(1990, 1, 1)
        val søkerFnr =
            leggTilPersonInfo(
                fødselsdatoSøker,
                PersonInfo(
                    fødselsdato = fødselsdatoSøker,
                    bostedsadresser = listOf(Bostedsadresse()) + defaultBostedsadresseHistorikk(fødselsdatoSøker),
                ),
            )
        val søkerAktør = personidentService.hentOgLagreAktør(søkerFnr, true)

        val fødselsdatoBarn = LocalDate.of(2009, 1, 1)
        val barnFnr =
            leggTilPersonInfo(
                fødselsdatoBarn,
                PersonInfo(
                    fødselsdato = fødselsdatoBarn,
                    bostedsadresser = listOf(Bostedsadresse()) + defaultBostedsadresseHistorikk(fødselsdatoBarn),
                ),
            )
        val barn1Aktør = personidentService.hentOgLagreAktør(barnFnr, true)

        val fagsak = fagsakService.hentEllerOpprettFagsak(FagsakRequest(personIdent = søkerAktør.aktivFødselsnummer()))
        val behandling =
            behandlingService.opprettBehandling(
                nyOrdinærBehandling(
                    søkersIdent = søkerAktør.aktivFødselsnummer(),
                    fagsakId = fagsak.data!!.id,
                ),
            )

        val personopplysningGrunnlag =
            persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                søkerAktør,
                listOf(barn1Aktør),
                behandling,
                Målform.NB,
            )

        personopplysningGrunnlag.personer.forEach {
            assertEquals(defaultBostedsadresseHistorikk(it.fødselsdato).size, it.bostedsadresser.size)
        }
    }
}
