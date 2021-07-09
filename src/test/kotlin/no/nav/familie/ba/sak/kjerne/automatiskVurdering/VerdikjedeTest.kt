package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.internal.Personident
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.junit.jupiter.api.Assertions
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
class VerdikjedeTest(
        @Autowired val stegService: StegService,
        @Autowired val personopplysningerService: PersonopplysningerService,
        @Autowired val persongrunnlagService: PersongrunnlagService,
        @Autowired val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        @Autowired val databaseCleanupService: DatabaseCleanupService,
) {

    @Test
    fun `Passerer vilkårsvurdering`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        every { personopplysningerService.hentPersoninfoMedRelasjoner(søkerFnr) } returns mockSøkerAutomatiskBehandling
        every { personopplysningerService.hentPersoninfoMedRelasjoner(barnFnr) } returns mockBarnAutomatiskBehandling
        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår = stegService.håndterVilkårsvurdering(behandlingFørVilkår)
        Assertions.assertEquals(BehandlingResultat.INNVILGET, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Skal ikke passere vilkårsvurdering dersom barn er over 18`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val barn = genererAutomatiskTestperson(LocalDate.parse("1999-10-10"), emptySet(), emptyList())
        val søker = genererAutomatiskTestperson(LocalDate.parse("1998-10-10"),
                                                setOf(ForelderBarnRelasjon(Personident(barnFnr),
                                                                           FORELDERBARNRELASJONROLLE.BARN)),
                                                emptyList())
        every { personopplysningerService.hentPersoninfoMedRelasjoner(barnFnr) } returns barn
        every { personopplysningerService.hentPersoninfoMedRelasjoner(søkerFnr) } returns søker

        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår = stegService.håndterVilkårsvurdering(behandlingFørVilkår)
        Assertions.assertEquals(BehandlingResultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Skal ikke passere vilkårsvurdering dersom barn ikke bor med mor`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val barn = genererAutomatiskTestperson(LocalDate.now(), emptySet(), emptyList())
        val søker = genererAutomatiskTestperson(LocalDate.parse("1998-10-10"),
                                                setOf(ForelderBarnRelasjon(Personident(barnFnr),
                                                                           FORELDERBARNRELASJONROLLE.BARN)),
                                                emptyList(),
                                                bostedsadresse = listOf(Bostedsadresse(
                                                        gyldigFraOgMed = null,
                                                        gyldigTilOgMed = null,
                                                        vegadresse = Vegadresse(matrikkelId = 1111111111,
                                                                                husnummer = "36",
                                                                                husbokstav = "D",
                                                                                bruksenhetsnummer = null,
                                                                                adressenavn = "IkkeSamme -veien",
                                                                                kommunenummer = "5423",
                                                                                tilleggsnavn = null,
                                                                                postnummer = "9050"),
                                                        matrikkeladresse = null,
                                                        ukjentBosted = null,
                                                )))

        every { personopplysningerService.hentPersoninfoMedRelasjoner(barnFnr) } returns barn
        every { personopplysningerService.hentPersoninfoMedRelasjoner(søkerFnr) } returns søker

        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(barnFnr))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår = stegService.håndterVilkårsvurdering(behandlingFørVilkår)
        Assertions.assertEquals(BehandlingResultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }
}