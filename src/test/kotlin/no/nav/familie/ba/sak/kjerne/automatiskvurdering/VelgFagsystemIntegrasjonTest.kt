package no.nav.familie.ba.sak.kjerne.automatiskvurdering

import io.mockk.every
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fødselshendelse.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.steg.StegService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
        properties = [
            "DAGLIG_KVOTE_FØDSELSHENDELSER: 0"
        ],
)
class VelgFagsystemIntegrasjonTest(
        @Autowired val stegService: StegService,
        @Autowired val personopplysningerService: PersonopplysningerService,
        @Autowired val persongrunnlagService: PersongrunnlagService,
        @Autowired val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        @Autowired val velgFagSystemService: VelgFagSystemService,
        @Autowired val fagSakService: FagsakService,
        @Autowired val infotrygdService: InfotrygdService,
        @Autowired val databaseCleanupService: DatabaseCleanupService
) : AbstractSpringIntegrationTest() {

    val søkerFnr = randomFnr()

    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `sjekk om mor har løpende utbetalinger i infotrygd`() {
        every { infotrygdService.harLøpendeSakIInfotrygd(any(), any()) } returns true

        assertEquals(true, velgFagSystemService.morEllerBarnHarLøpendeSakIInfotrygd(søkerFnr, emptyList()))
    }

    @Test
    fun `skal velge ba-sak når mor ikke har noe løpende i Infotrygd, men daglig kvote er oppbrukt`() {
        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(søkerFnr))

        assertEquals(FagsystemRegelVurdering.SEND_TIL_INFOTRYGD, velgFagSystemService.velgFagsystem(nyBehandling))
    }
}