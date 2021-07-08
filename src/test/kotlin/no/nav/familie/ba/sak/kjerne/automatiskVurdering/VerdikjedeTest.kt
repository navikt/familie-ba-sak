package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
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
class Integrasjonstest(
        @Autowired val stegService: StegService,
        @Autowired val personopplysningerService: PersonopplysningerService,
        @Autowired val persongrunnlagService: PersongrunnlagService,
        @Autowired val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        @Autowired val pdlRestClient: PdlRestClient,
) {

    @Test
    fun `Passerer vilkårsvurdering`() {
        every { personopplysningerService.hentPersoninfoMedRelasjoner("04086226621") } returns mockSøkerAutomatiskBehandling
        every { personopplysningerService.hentPersoninfoMedRelasjoner("21111777001") } returns mockBarnAutomatiskBehandling
        /*
        every { personopplysningerService.hentPersoninfo("04086226621") } returns mockSøkerAutomatiskBehandling
        every { personopplysningerService.hentHistoriskPersoninfoManuell("04086226621") } returns mockSøkerAutomatiskBehandling
        every { pdlRestClient.hentPerson("21111777001", any()) } returns mockBarnAutomatiskBehandling
        every { pdlRestClient.hentPerson("04086226621", any()) } returns mockSøkerAutomatiskBehandling
         */
        val nyBehandling = NyBehandlingHendelse("04086226621", listOf("21111777001"))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår = stegService.håndterVilkårsvurdering(behandlingFørVilkår)
        val personopplysningsgrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingEtterVilkår.id)
        Assertions.assertEquals(BehandlingResultat.INNVILGET, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Skal ikke passere vilkårsvurdering dersom barn er over 18`() {
        val barn = genererAutomatiskTestperson(LocalDate.parse("1999-10-10"), emptySet(), emptyList())
        val søker = genererAutomatiskTestperson(LocalDate.parse("1998-10-10"), setOf("12345678910"), emptyList())
        every { personopplysningerService.hentPersoninfoMedRelasjoner("11211211211") } returns barn
        every { personopplysningerService.hentPersoninfoMedRelasjoner("10987654321") } returns søker

        val nyBehandling = NyBehandlingHendelse("10987654321", listOf("11211211211"))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår = stegService.håndterVilkårsvurdering(behandlingFørVilkår)
        val personopplysningsgrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingEtterVilkår.id)
        Assertions.assertEquals(BehandlingResultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }

    @Test
    fun `Skal ikke passere vilkårsvurdering dersom barn ikke bor med mor`() {
        val barn = genererAutomatiskTestperson(LocalDate.now(), emptySet(), emptyList())
        val søker = genererAutomatiskTestperson(LocalDate.parse("1998-10-10"), setOf("12345678910"), emptyList())
        every { personopplysningerService.hentPersoninfoMedRelasjoner("11211211211") } returns barn
        every { personopplysningerService.hentPersoninfoMedRelasjoner("10987654321") } returns søker

        val nyBehandling = NyBehandlingHendelse("10987654321", listOf("11211211211"))
        val behandlingFørVilkår = stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForHendelse(nyBehandling)
        val behandlingEtterVilkår = stegService.håndterVilkårsvurdering(behandlingFørVilkår)
        val personopplysningsgrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingEtterVilkår.id)
        Assertions.assertEquals(BehandlingResultat.AVSLÅTT, behandlingEtterVilkår.resultat)
    }
}