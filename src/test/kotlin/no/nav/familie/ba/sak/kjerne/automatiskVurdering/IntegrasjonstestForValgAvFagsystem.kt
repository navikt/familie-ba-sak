package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import io.mockk.every
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.e2e.DatabaseCleanupService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.FagsystemRegelVurdering
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

class Integrasjonstest(
        @Autowired val stegService: StegService,
        @Autowired val personopplysningerService: PersonopplysningerService,
        @Autowired val persongrunnlagService: PersongrunnlagService,
        @Autowired val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
        @Autowired val velgFagSystemService: VelgFagSystemService,
        @Autowired val fagSakService: FagsakService,
        @Autowired val infotrygdService: InfotrygdService,
        @Autowired val databaseCleanupService: DatabaseCleanupService
): AbstractSpringIntegrationTest() {

    val søkerFnr = randomFnr()

    @BeforeAll
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `sjekk om mor har løpende utbetalinger i infotrygd`() {
        every { infotrygdService.harLøpendeSakIInfotrygd(mutableListOf(søkerFnr)) } returns true

        assertEquals(true, velgFagSystemService.morHarLøpendeSakIInfotrygd(søkerFnr))
    }

    @Test
    fun `skal velge Infotrygd når mor ikke har løpende sak i BA sak`() {

        fagSakService.hentEllerOpprettFagsak(PersonIdent(søkerFnr), true)
        val nyBehandling = NyBehandlingHendelse(søkerFnr, listOf(søkerFnr))


        assertEquals(velgFagSystemService.velgFagsystem(nyBehandling), FagsystemRegelVurdering.SEND_TIL_INFOTRYGD)
    }
}