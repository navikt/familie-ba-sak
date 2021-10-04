package no.nav.familie.ba.sak.ekstern.skatteetaten

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerson
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPersonerResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class SkatteetatenServiceTest {

    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient = mockk()
    private val fagsakRepository: FagsakRepository = mockk()

    @Test
    fun `finnPersonerMedUtvidetBarnetrygd skal returnere person fra fagsystem med nyeste vedtaksdato`() {
        val perosnIdent = randomFnr()
        val fagsak = defaultFagsak()
        val fagsak2 = defaultFagsak()
        Fagsak(1, FagsakStatus.LØPENDE)

        val nyesteVedtaksdato = LocalDate.now()
        every { fagsakRepository.finnFagsakerMedUtvidetBarnetrygdInnenfor(any(), any()) } returns listOf(
            fagsak to nyesteVedtaksdato,
            fagsak2 to nyesteVedtaksdato.plusDays(2)
        )
        every { infotrygdBarnetrygdClient.hentPersonerMedUtvidetBarnetrygd(any()) } returns SkatteetatenPersonerResponse( listOf(
            SkatteetatenPerson(fagsak.hentAktivIdent().ident, LocalDateTime.now().minusYears(1))
        ))

        val skatteetatenService = SkatteetatenService(infotrygdBarnetrygdClient, fagsakRepository)

        assertThat(skatteetatenService.finnPersonerMedUtvidetBarnetrygd("2020").brukere
                       .first { it.ident == fagsak.hentAktivIdent().ident }.sisteVedtakPaaIdent)
            .isEqualTo(nyesteVedtaksdato.atStartOfDay())
    }
}