package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.Personident
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest(properties = ["FAMILIE_FAMILIE_TILBAKE_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles(
        "dev",
        "postgress",
        "mock-pdl",
        "mock-familie-tilbake",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
)
@Tag("integration")
class Integrasjonstest(
        @Autowired val stegService: StegService,
        @Autowired val personopplysningerService: PersonopplysningerService,
        @Autowired val persongrunnlagService: PersongrunnlagService,
        @Autowired val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        @Autowired val pdlRestClient: PdlRestClient,
        @Autowired val velgFagSystemService: VelgFagSystemService,
        @Autowired val fagSakService: FagsakService
) {

    val etBarnsIdent = "11211211211"
    val enMorsIdent = "10987654321"

    @Test
    fun `Passerer vilkårsvurdering`() {
        val morsIdent = "04086226621"
        val barnasIdenter = listOf("21111777001")
        every { personopplysningerService.hentPersoninfoMedRelasjoner(morsIdent) } returns mockSøkerAutomatiskBehandling
        every { personopplysningerService.hentPersoninfoMedRelasjoner(barnasIdenter.first()) } returns mockBarnAutomatiskBehandling
        val nyBehandling = NyBehandlingHendelse(morsIdent, barnasIdenter)
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår = stegService.håndterVilkårsvurdering(behandlingFørVilkår)
        val personopplysningsgrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingEtterVilkår.id)
        assertEquals(BehandlingResultat.INNVILGET, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Skal ikke passere vilkårsvurdering dersom barn er over 18`() {
        val barn = genererAutomatiskTestperson(LocalDate.parse("1999-10-10"), emptySet(), emptyList())
        val søker = genererAutomatiskTestperson(LocalDate.parse("1998-10-10"),
                                                setOf(ForelderBarnRelasjon(Personident(etBarnsIdent),
                                                                           FORELDERBARNRELASJONROLLE.BARN)),
                                                emptyList())
        every { personopplysningerService.hentPersoninfoMedRelasjoner(etBarnsIdent) } returns barn
        every { personopplysningerService.hentPersoninfoMedRelasjoner(enMorsIdent) } returns søker

        val nyBehandling = NyBehandlingHendelse(enMorsIdent, listOf(etBarnsIdent))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår = stegService.håndterVilkårsvurdering(behandlingFørVilkår)
        val personopplysningsgrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingEtterVilkår.id)
        assertEquals(BehandlingResultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Skal ikke passere vilkårsvurdering dersom barn ikke bor med mor`() {
        val barn = genererAutomatiskTestperson(
            LocalDate.now(), emptySet(), emptyList(), listOf(
                Bostedsadresse(
                    gyldigFraOgMed = null,
                    gyldigTilOgMed = null,
                    vegadresse = Vegadresse(
                        matrikkelId = 1111111111,
                        husnummer = "36",
                        husbokstav = "D",
                        bruksenhetsnummer = null,
                        adressenavn = "Ikke samme -veien",
                        kommunenummer = "5422",
                        tilleggsnavn = null,
                        postnummer = "9050"
                    ),
                )
            )
        )
        val søker = genererAutomatiskTestperson(
            LocalDate.parse("1998-10-10"),
            setOf(
                ForelderBarnRelasjon(
                    Personident(etBarnsIdent),
                    FORELDERBARNRELASJONROLLE.BARN
                )
            ),
            emptyList()
        )
        every { personopplysningerService.hentPersoninfoMedRelasjoner(etBarnsIdent) } returns barn
        every { personopplysningerService.hentPersoninfoMedRelasjoner(enMorsIdent) } returns søker

        val nyBehandling = NyBehandlingHendelse(enMorsIdent, listOf(etBarnsIdent))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår = stegService.håndterVilkårsvurdering(behandlingFørVilkår)
        val personopplysningsgrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingEtterVilkår.id)
        assertEquals(BehandlingResultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `skal velge BAsak når mor har løpende sak i BA sak`() {

        val fagsak = fagSakService.hentEllerOpprettFagsak(PersonIdent(enMorsIdent), true)
        fagSakService.oppdaterStatus(fagsak, FagsakStatus.LØPENDE)

        val nyBehandling = NyBehandlingHendelse(enMorsIdent, listOf(etBarnsIdent))

        assertEquals(velgFagSystemService.velgFagsystem(nyBehandling), FagsystemRegelVurdering.SEND_TIL_BA)
    }

    @Test
    fun `skal velge Infotrygd når mor ikke har løpende sak i BA sak`() {

        fagSakService.hentEllerOpprettFagsak(PersonIdent(enMorsIdent), true)
        val nyBehandling = NyBehandlingHendelse(enMorsIdent, listOf(etBarnsIdent))


        assertEquals(velgFagSystemService.velgFagsystem(nyBehandling), FagsystemRegelVurdering.SEND_TIL_INFOTRYGD)
    }
}