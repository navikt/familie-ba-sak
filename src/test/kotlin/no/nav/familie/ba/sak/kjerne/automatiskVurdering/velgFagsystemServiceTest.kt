package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.VelgFagSystemService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

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
class velgFagsystemServiceTest(
        @Autowired val velgFagSystemService: VelgFagSystemService
) {


    @Test
    fun `sjekk om mor har løpende utbetaling i BA-sak`() {

        Assertions.assertTrue(velgFagSystemService.morHarLøpendeUtbetalingerIBA(defaultFagsak.copy(status = FagsakStatus.LØPENDE)))
        Assertions.assertFalse(velgFagSystemService.morHarLøpendeUtbetalingerIBA(defaultFagsak.copy(status = FagsakStatus.OPPRETTET)))
    }

    @Test
    fun `sjekk om mor har løpende utbetalinger i infotrygd`() {
    }

    @Test
    fun `sjekk om mor har saker men ikke løpende utbetalinger i BA-sak`() {
        Assertions.assertTrue(velgFagSystemService.morHarSakerMenIkkeLøpendeUtbetalingerIBA(defaultFagsak))
    }

    @Test
    fun `sjekk om mor har saker men ikke løpende utbetalinger i Infotrygd`() {

    }

    @Test
    fun `sjekk om mor har barn der far har løpende utbetaling i infotrygd`() {

    }

}