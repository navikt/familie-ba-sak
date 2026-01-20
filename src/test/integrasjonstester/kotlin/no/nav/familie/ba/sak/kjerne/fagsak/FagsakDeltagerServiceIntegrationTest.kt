package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.randomSøkerFødselsdato
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerDto
import no.nav.familie.ba.sak.fake.FakePdlIdentRestKlient
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonIkkeFunnet
import no.nav.familie.ba.sak.fake.FakePersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.RegistrerPersongrunnlagDTO
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType.SAK
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class FagsakDeltagerServiceIntegrationTest(
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val fagsakDeltagerService: FagsakDeltagerService,
    @Autowired
    private val behandlingService: BehandlingService,
    @Autowired
    private val personidentService: PersonidentService,
    @Autowired
    private val stegService: StegService,
    @Autowired
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
    @Autowired
    private val fakePdlIdentRestKlient: FakePdlIdentRestKlient,
) : AbstractSpringIntegrationTest() {
    /*
        This is a complicated test against following family relationship:
        søker3-----------
        (no case)       | (medmor)
                        barn2
                        | (medmor)
        søker1-----------
                        | (mor)
                        barn1
                        | (far)
        søker2-----------
                        | (far)
                        barn3

         We tests three search:
         1) search for søker1, one participant (søker1) should be returned
         2) search for barn1, three participants (barn1, søker1, søker2) should be returned
         3) search for barn2, three participants (barn2, søker3, søker1) should be returned, where fagsakId of søker3 is null
     */
    @Test
    fun `test å søke fagsak med fnr`() {
        val søker1Fødselsdato = LocalDate.of(1990, 2, 19)
        val søker1Fnr =
            leggTilPersonInfo(
                søker1Fødselsdato,
                PersonInfo(fødselsdato = søker1Fødselsdato, kjønn = Kjønn.KVINNE, navn = "søker1"),
            )
        val søker1Aktør = personidentService.hentAktør(søker1Fnr)

        val søker2Fødselsdato = LocalDate.of(1991, 2, 20)
        val søker2Fnr =
            leggTilPersonInfo(
                søker2Fødselsdato,
                PersonInfo(fødselsdato = søker2Fødselsdato, kjønn = Kjønn.MANN, navn = "søker2"),
            )

        val søker3Fødselsdato = LocalDate.of(1990, 1, 10)
        val søker3Fnr =
            leggTilPersonInfo(
                søker3Fødselsdato,
                PersonInfo(fødselsdato = søker3Fødselsdato, kjønn = Kjønn.KVINNE, navn = "søker3"),
            )
        val søker3Aktør = personidentService.hentAktør(søker3Fnr)

        val barn1Fødselsdato = LocalDate.of(2018, 5, 1)
        val barn1Fnr =
            leggTilPersonInfo(
                barn1Fødselsdato,
                PersonInfo(fødselsdato = barn1Fødselsdato, kjønn = Kjønn.KVINNE, navn = "barn1"),
            )

        val barn2Fødselsdato = LocalDate.of(2019, 5, 1)
        val barn2Fnr =
            leggTilPersonInfo(
                barn2Fødselsdato,
                PersonInfo(
                    fødselsdato = barn2Fødselsdato,
                    kjønn = Kjønn.MANN,
                    navn = "barn2",
                    forelderBarnRelasjon =
                        setOf(
                            ForelderBarnRelasjon(
                                søker1Aktør,
                                FORELDERBARNRELASJONROLLE.MEDMOR,
                                "søker1",
                                søker1Fødselsdato,
                            ),
                            ForelderBarnRelasjon(
                                søker3Aktør,
                                FORELDERBARNRELASJONROLLE.MEDMOR,
                                "søker3",
                                søker2Fødselsdato,
                            ),
                        ),
                ),
            )

        val barn3fødselsdato = LocalDate.of(2017, 3, 1)
        leggTilPersonInfo(
            barn3fødselsdato,
            PersonInfo(fødselsdato = barn3fødselsdato, kjønn = Kjønn.KVINNE, navn = "barn3"),
        )

        val fagsak0 =
            fagsakService.hentEllerOpprettFagsak(
                FagsakRequest(
                    søker1Fnr,
                ),
            )

        val fagsak1 =
            fagsakService.hentEllerOpprettFagsak(
                FagsakRequest(
                    søker2Fnr,
                ),
            )

        val førsteBehandling =
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    søkersIdent = søker1Fnr,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    søknadMottattDato = LocalDate.now(),
                    fagsakId = fagsak0.data!!.id,
                ),
            )
        stegService.håndterPersongrunnlag(
            førsteBehandling,
            RegistrerPersongrunnlagDTO(ident = søker1Fnr, barnasIdenter = listOf(barn1Fnr)),
        )

        behandlingService.oppdaterStatusPåBehandling(førsteBehandling.id, BehandlingStatus.AVSLUTTET)

        val andreBehandling =
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    søkersIdent = søker1Fnr,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    søknadMottattDato = LocalDate.now(),
                    fagsakId = fagsak0.data!!.id,
                ),
            )
        stegService.håndterPersongrunnlag(
            andreBehandling,
            RegistrerPersongrunnlagDTO(
                ident = søker1Fnr,
                barnasIdenter = listOf(barn1Fnr, barn2Fnr),
            ),
        )

        val tredjeBehandling =
            stegService.håndterNyBehandling(
                NyBehandling(
                    kategori = BehandlingKategori.NASJONAL,
                    underkategori = BehandlingUnderkategori.ORDINÆR,
                    søkersIdent = søker2Fnr,
                    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                    søknadMottattDato = LocalDate.now(),
                    fagsakId = fagsak1.data!!.id,
                ),
            )
        stegService.håndterPersongrunnlag(
            tredjeBehandling,
            RegistrerPersongrunnlagDTO(ident = søker2Fnr, barnasIdenter = listOf(barn1Fnr)),
        )

        val søkeresultat1 = fagsakDeltagerService.hentFagsakDeltagere(søker1Fnr)
        assertEquals(1, søkeresultat1.size)
        assertEquals(Kjønn.KVINNE, søkeresultat1[0].kjønn)
        assertEquals("søker1", søkeresultat1[0].navn)
        assertEquals(fagsak0.data!!.id, søkeresultat1[0].fagsakId)

        val søkeresultat2 = fagsakDeltagerService.hentFagsakDeltagere(barn1Fnr)
        assertEquals(3, søkeresultat2.size)
        var matching = 0
        søkeresultat2.forEach {
            matching +=
                if (it.fagsakId == fagsak0.data!!.id) {
                    1
                } else if (it.fagsakId == fagsak1.data!!.id) {
                    10
                } else {
                    0
                }
        }
        assertEquals(11, matching)
        assertEquals(1, søkeresultat2.filter { it.ident == barn1Fnr }.size)

        val søkeresultat3 = fagsakDeltagerService.hentFagsakDeltagere(barn2Fnr)
        assertEquals(3, søkeresultat3.size)
        assertEquals(1, søkeresultat3.filter { it.ident == barn2Fnr }.size)
        assertNull(søkeresultat3.find { it.ident == barn2Fnr }!!.fagsakId)
        assertEquals(fagsak0.data!!.id, søkeresultat3.find { it.ident == søker1Fnr }!!.fagsakId)
        assertEquals(1, søkeresultat3.filter { it.ident == søker3Fnr }.size)
        assertEquals("søker3", søkeresultat3.filter { it.ident == søker3Fnr }[0].navn)
        assertNull(søkeresultat3.find { it.ident == søker3Fnr }!!.fagsakId)

        val fagsak = fagsakService.hentNormalFagsak(søker1Aktør)!!

        assertEquals(
            FagsakStatus.OPPRETTET.name,
            saksstatistikkMellomlagringRepository
                .findByTypeAndTypeId(SAK, fagsak.id)
                .last()
                .jsonToSakDVH()
                .sakStatus,
        )
    }

    @Test
    fun `Skal teste at arkiverte fagsaker med behandling ikke blir funnet ved søk`() {
        val søker1Fnr =
            leggTilPersonInfo(
                randomSøkerFødselsdato(),
                PersonInfo(fødselsdato = LocalDate.of(1991, 2, 19), kjønn = Kjønn.KVINNE, navn = "søker1"),
            )
        val søker1Aktør = lagAktør(søker1Fnr)

        val fagsak =
            fagsakService.hentEllerOpprettFagsak(
                FagsakRequest(
                    søker1Fnr,
                ),
            )

        stegService.håndterNyBehandling(
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = søker1Fnr,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                søknadMottattDato = LocalDate.now(),
                fagsakId = fagsak.data!!.id,
            ),
        )

        fagsakService.lagre(
            fagsakService.hentFagsakPåPerson(søker1Aktør).also { it?.arkivert = true }!!,
        )

        val søkeresultat1 = fagsakDeltagerService.hentFagsakDeltagere(søker1Fnr)

        assertEquals(1, søkeresultat1.size)
        assertNull(søkeresultat1.first().fagsakId)
    }

    @Test
    fun `Skal teste at arkiverte fagsaker uten behandling ikke blir funnet ved søk`() {
        val søker1Fnr =
            leggTilPersonInfo(
                randomSøkerFødselsdato(),
                PersonInfo(fødselsdato = LocalDate.of(1992, 2, 19), kjønn = Kjønn.KVINNE, navn = "søker1"),
            )
        val søker1Aktør = lagAktør(søker1Fnr)

        fagsakService.hentEllerOpprettFagsak(
            FagsakRequest(
                søker1Fnr,
            ),
        )

        fagsakService.lagre(
            fagsakService.hentFagsakPåPerson(søker1Aktør).also { it?.arkivert = true }!!,
        )

        val søkeresultat1 = fagsakDeltagerService.hentFagsakDeltagere(søker1Fnr)

        assertEquals(1, søkeresultat1.size)
        assertNull(søkeresultat1.first().fagsakId)
    }

    @Test
    fun `Skal returnere tom liste ved søk hvis ident ikke har aktiv fødselsnummer`() {
        val fnr = randomFnr()
        fakePdlIdentRestKlient.leggTilIdent(
            fnr,
            listOf(
                IdentInformasjon("npid", gruppe = "NPID", historisk = false),
                IdentInformasjon("122334343", gruppe = "AKTOERID", historisk = false),
            ),
        )
        assertThat(fagsakDeltagerService.hentFagsakDeltagere(fnr)).hasSize(0)
    }

    @Test
    fun `Søk på fnr som ikke finnes i PDL skal vi tom liste`() {
        val aktør = lagAktør()

        leggTilPersonIkkeFunnet(aktør.aktivFødselsnummer())
        assertEquals(emptyList<FagsakDeltagerDto>(), fagsakDeltagerService.hentFagsakDeltagere(aktør.aktivFødselsnummer()))
    }
}
