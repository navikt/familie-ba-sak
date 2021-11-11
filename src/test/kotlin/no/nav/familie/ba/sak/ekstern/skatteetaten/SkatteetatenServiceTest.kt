package no.nav.familie.ba.sak.ekstern.skatteetaten

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.unmockkAll
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerson
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPersonerResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SkatteetatenServiceTest {

    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient = mockk()
    private val fagsakRepository: FagsakRepository = mockk()
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository = mockk()

    @AfterAll
    fun tearDown() {
        // HER?
        unmockkAll()
    }

    @Test
    fun `finnPersonerMedUtvidetBarnetrygd() skal returnere person fra fagsystem med nyeste vedtaksdato`() {
        val fagsak = defaultFagsak()
        val fagsak2 = defaultFagsak()

        val nyesteVedtaksdato = LocalDate.now()
        every { fagsakRepository.finnFagsakerMedUtvidetBarnetrygdInnenfor(any(), any()) } returns listOf(
            fagsak to nyesteVedtaksdato,
            fagsak2 to nyesteVedtaksdato.plusDays(2)
        )
        every { infotrygdBarnetrygdClient.hentPersonerMedUtvidetBarnetrygd(any()) } returns SkatteetatenPersonerResponse(
            listOf(
                SkatteetatenPerson(fagsak.hentAktivIdent().ident, nyesteVedtaksdato.atStartOfDay().minusYears(1))
            )
        )

        val skatteetatenService =
            SkatteetatenService(infotrygdBarnetrygdClient, fagsakRepository, andelTilkjentYtelseRepository)

        assertThat(skatteetatenService.finnPersonerMedUtvidetBarnetrygd(nyesteVedtaksdato.year.toString()).brukere).hasSize(
            2
        )

        assertThat(
            skatteetatenService.finnPersonerMedUtvidetBarnetrygd(nyesteVedtaksdato.year.toString()).brukere
                .find { it.ident == fagsak.hentAktivIdent().ident }!!.sisteVedtakPaaIdent
        )
            .isEqualTo(nyesteVedtaksdato.atStartOfDay())

        assertThat(
            skatteetatenService.finnPersonerMedUtvidetBarnetrygd(nyesteVedtaksdato.year.toString()).brukere
                .find { it.ident == fagsak2.hentAktivIdent().ident }!!.sisteVedtakPaaIdent
        )
            .isEqualTo(nyesteVedtaksdato.plusDays(2).atStartOfDay())
    }

    @Test
    fun `finnPersonerMedUtvidetBarnetrygd() return kun resultat fra ba-sak når ingen treff i infotrygd`() {
        every { infotrygdBarnetrygdClient.hentPersonerMedUtvidetBarnetrygd(any()) } returns
            SkatteetatenPersonerResponse(brukere = emptyList())

        val fagsak = defaultFagsak()
        val fagsak2 = defaultFagsak()

        val vedtaksdato = LocalDate.now()
        every { fagsakRepository.finnFagsakerMedUtvidetBarnetrygdInnenfor(any(), any()) } returns listOf(
            fagsak to vedtaksdato,
            fagsak2 to vedtaksdato.plusDays(2)
        )

        val skatteetatenService =
            SkatteetatenService(infotrygdBarnetrygdClient, fagsakRepository, andelTilkjentYtelseRepository)
        val personerMedUtvidetBarnetrygd =
            skatteetatenService.finnPersonerMedUtvidetBarnetrygd(vedtaksdato.year.toString())

        assertThat(personerMedUtvidetBarnetrygd.brukere).hasSize(2)

        assertThat(
            personerMedUtvidetBarnetrygd.brukere
                .find { it.ident == fagsak.hentAktivIdent().ident }!!.sisteVedtakPaaIdent
        )
            .isEqualTo(vedtaksdato.atStartOfDay())

        assertThat(
            personerMedUtvidetBarnetrygd.brukere
                .find { it.ident == fagsak2.hentAktivIdent().ident }!!.sisteVedtakPaaIdent
        )
            .isEqualTo(vedtaksdato.plusDays(2).atStartOfDay())
    }

    @Test
    fun `finnPersonerMedUtvidetBarnetrygd() skal return kun resultat fra infotrygd når ingen treff i ba-sak`() {
        every { fagsakRepository.finnFagsakerMedUtvidetBarnetrygdInnenfor(any(), any()) } returns emptyList()

        val fagsak = defaultFagsak()
        val vedtaksdato = LocalDate.now()

        every { infotrygdBarnetrygdClient.hentPersonerMedUtvidetBarnetrygd(any()) } returns
            SkatteetatenPersonerResponse(
                brukere = listOf(
                    SkatteetatenPerson(
                        fagsak.hentAktivIdent().ident,
                        vedtaksdato.atStartOfDay()
                    )
                )
            )

        val skatteetatenService =
            SkatteetatenService(infotrygdBarnetrygdClient, fagsakRepository, andelTilkjentYtelseRepository)
        val personerMedUtvidetBarnetrygd =
            skatteetatenService.finnPersonerMedUtvidetBarnetrygd(vedtaksdato.year.toString())

        assertThat(personerMedUtvidetBarnetrygd.brukere).hasSize(1)

        assertThat(
            personerMedUtvidetBarnetrygd.brukere
                .find { it.ident == fagsak.hentAktivIdent().ident }!!.sisteVedtakPaaIdent
        )
            .isEqualTo(vedtaksdato.atStartOfDay())
    }
}
