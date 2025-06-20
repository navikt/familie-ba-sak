package no.nav.familie.ba.sak.kjerne.minside

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MinsideAktiveringAktørValidatorTest {
    private val fagsakService: FagsakService = mockk()
    private val minsideAktiveringAktørValidator = MinsideAktiveringAktørValidator(fagsakService = fagsakService)

    @Nested
    inner class KanAktivereMinsideForAktør {
        @Test
        fun `skal returnere false dersom aktør ikke har noen fagsaker`() {
            // Arrange
            val aktør = randomAktør()

            every { fagsakService.hentAlleFagsakerForAktør(aktør) } returns emptyList()

            // Act
            val kanAktivere = minsideAktiveringAktørValidator.kanAktivereMinsideForAktør(aktør)

            // Assert
            assertThat(kanAktivere).isFalse
        }
    }

    @Test
    fun `skal returnere false dersom aktør kun har fagsaker av typene SKJERMET_BARN eller INSTITUSJON`() {
        // Arrange
        val aktør = randomAktør()

        every { fagsakService.hentAlleFagsakerForAktør(aktør) } returns
            listOf(
                lagFagsak(type = FagsakType.SKJERMET_BARN, aktør = aktør),
                lagFagsak(type = FagsakType.INSTITUSJON, aktør = aktør),
            )

        // Act
        val kanAktivere = minsideAktiveringAktørValidator.kanAktivereMinsideForAktør(aktør)

        // Assert
        assertThat(kanAktivere).isFalse
    }

    @Test
    fun `skal returnere true dersom aktør har fagsaker av andre typer enn SKJERMET_BARN eller INSTITUSJON`() {
        // Arrange
        val aktør = randomAktør()

        every { fagsakService.hentAlleFagsakerForAktør(aktør) } returns
            listOf(
                lagFagsak(type = FagsakType.NORMAL, aktør = aktør),
            )

        // Act
        val kanAktivere = minsideAktiveringAktørValidator.kanAktivereMinsideForAktør(aktør)

        // Assert
        assertThat(kanAktivere).isTrue
    }
}
