package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.mockk
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.morHarLøpendeUtbetalingerIBA
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.morHarSakerMenIkkeLøpendeUtbetalingerIBA
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fødselshendelse.OppgaveBeskrivelseTest
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@SpringBootTest(properties = ["FAMILIE_FAMILIE_TILBAKE_API_URL=http://localhost:28085/api"])
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@AutoConfigureWireMock(port = 28085)
@ActiveProfiles(
        "postgres",
        "mock-tilbakekreving-klient",
        "mock-infotrygd-barnetrygd",
        "mock-pdl"
)
@Tag("integration")
class velgFagsystemServiceTest(
        @Autowired private val fagsakService: FagsakService,
        @Autowired private val infotrygdService: InfotrygdService
) {


    private val fagsakRepositoryMock = mockk<FagsakRepository>()

    private val personopplysningGrunnlagMock = mockk<PersonopplysningGrunnlag>()
    private val personopplysningserviceMock = mockk<PersonopplysningerService>()

    val søker = Person(type = PersonType.SØKER,
                       personIdent = PersonIdent("12345678910"),
                       fødselsdato = LocalDate.of(1990, 1, 12),
                       kjønn = Kjønn.KVINNE,
                       personopplysningGrunnlag = personopplysningGrunnlagMock)
            .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.GIFT, person = this)) }

    val barn = Person(type = PersonType.BARN, fødselsdato = LocalDate.of(2019, 1, 12), kjønn = Kjønn.KVINNE,
                      personIdent = PersonIdent(OppgaveBeskrivelseTest.barnetsIdent),
                      personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = OppgaveBeskrivelseTest.behandling.id))
            .apply { sivilstander = listOf(GrSivilstand(type = SIVILSTAND.GIFT, person = this)) }


    val morsfagsak = fagsakService.hentEllerOpprettFagsak(søker.personIdent)

    @Test
    fun `sjekk om mor har løpende utbetaling i BA-sak`() {
        
        fagsakService.oppdaterStatus(morsfagsak, FagsakStatus.LØPENDE)
        Assertions.assertEquals(true, morHarLøpendeUtbetalingerIBA(morsfagsak))
    }

    @Test
    fun `sjekk om mor har løpende utbetalinger i infotrygd`() {
        Assertions.assertEquals(false, infotrygdService.harLøpendeSakIInfotrygd(mutableListOf(søker.personIdent.ident)))
    }

    @Test
    fun `sjekk om mor har saker men ikke løpende utbetalinger i BA-sak`() {
        Assertions.assertEquals(true, morHarSakerMenIkkeLøpendeUtbetalingerIBA(morsfagsak))
    }

    @Test
    fun `sjekk om mor har saker men ikke løpende utbetalinger i Infotrygd`() {

    }

    @Test
    fun `sjekk om mor har barn der far har løpende utbetaling i infotrygd`() {

    }

}