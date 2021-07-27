package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.morHarLøpendeUtbetalingerIBA
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.morHarSakerMenIkkeLøpendeUtbetalingerIBA
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class velgFagsystemUtilTest(
) {


    @Test
    fun `sjekk om mor har løpende utbetaling i BA-sak`() {

        Assertions.assertTrue(morHarLøpendeUtbetalingerIBA(defaultFagsak.copy(status = FagsakStatus.LØPENDE)))
        Assertions.assertFalse(morHarLøpendeUtbetalingerIBA(defaultFagsak.copy(status = FagsakStatus.OPPRETTET)))
    }


    @Test
    fun `sjekk om mor har saker men ikke løpende utbetalinger i BA-sak`() {
        Assertions.assertTrue(morHarSakerMenIkkeLøpendeUtbetalingerIBA(defaultFagsak))
    }


}