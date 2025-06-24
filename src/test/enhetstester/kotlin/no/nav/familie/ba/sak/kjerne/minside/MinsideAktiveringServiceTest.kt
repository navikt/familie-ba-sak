package no.nav.familie.ba.sak.kjerne.minside

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.randomAktør
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MinsideAktiveringServiceTest {
    private val minsideAktiveringRepository: MinsideAktiveringRepository = mockk()
    private val minsideAktiveringKafkaProducer: MinsideAktiveringKafkaProducer = mockk()
    private val minsideAktiveringService =
        MinsideAktiveringService(
            minsideAktiveringRepository = minsideAktiveringRepository,
            minsideAktiveringKafkaProducer = minsideAktiveringKafkaProducer,
        )

    @Nested
    inner class HarAktivertMinsideAktivering {
        @ParameterizedTest
        @ValueSource(booleans = [false, true])
        fun `skal returnere hvorvidt aktørens MinsideAktivering er aktivert`(aktivert: Boolean) {
            // Arrange
            val aktør = randomAktør()

            every { minsideAktiveringRepository.existsByAktørAndAktivertIsTrue(aktør) } returns aktivert

            // Act
            val harAktivertMinsideAktivering = minsideAktiveringService.harAktivertMinsideAktivering(aktør)

            // Assert
            assertThat(harAktivertMinsideAktivering).isEqualTo(aktivert)
        }
    }

    @Nested
    inner class AktiverMinsideAktivering {
        @Test
        fun `skal opprette ny aktiv MinsideAktivering for aktør dersom den ikke eksisterer fra før`() {
            // Arrange
            val aktør = randomAktør()
            val forventetMinsideAktivering = MinsideAktivering(aktør = aktør, aktivert = true)

            every { minsideAktiveringRepository.findByAktør(aktør) } returns null
            every { minsideAktiveringKafkaProducer.aktiver(aktør.aktivFødselsnummer()) } just Runs
            every { minsideAktiveringRepository.save(forventetMinsideAktivering) } returns forventetMinsideAktivering

            // Act
            val aktiverteMinsideAktivering = minsideAktiveringService.aktiverMinsideAktivering(aktør)

            // Assert
            assertThat(aktiverteMinsideAktivering).isEqualTo(forventetMinsideAktivering)
        }

        @Test
        fun `skal oppdatere aktiv MinsideAktivering for aktør dersom den eksisterer fra før`() {
            // Arrange
            val aktør = randomAktør()
            val eksisterendeMinsideAktivering = MinsideAktivering(id = 1, aktør = aktør, aktivert = false)
            val forventetMinsideAktivering = MinsideAktivering(id = 1, aktør = aktør, aktivert = true)

            every { minsideAktiveringRepository.findByAktør(aktør) } returns eksisterendeMinsideAktivering
            every { minsideAktiveringKafkaProducer.aktiver(aktør.aktivFødselsnummer()) } just Runs
            every { minsideAktiveringRepository.save(forventetMinsideAktivering) } returns forventetMinsideAktivering

            // Act
            val aktiverteMinsideAktivering = minsideAktiveringService.aktiverMinsideAktivering(aktør)

            // Assert
            assertThat(aktiverteMinsideAktivering).isEqualTo(forventetMinsideAktivering)
        }
    }

    @Nested
    inner class DeaktiverMinsideAktivering {
        @Test
        fun `skal opprette ny deaktivert MinsideAktivering for aktør dersom den ikke eksisterer fra før`() {
            // Arrange
            val aktør = randomAktør()
            val forventetMinsideAktivering = MinsideAktivering(aktør = aktør, aktivert = false)

            every { minsideAktiveringRepository.findByAktør(aktør) } returns null
            every { minsideAktiveringKafkaProducer.deaktiver(aktør.aktivFødselsnummer()) } just Runs
            every { minsideAktiveringRepository.save(forventetMinsideAktivering) } returns forventetMinsideAktivering

            // Act
            val deaktiverteMinsideAktivering = minsideAktiveringService.deaktiverMinsideAktivering(aktør)

            // Assert
            assertThat(deaktiverteMinsideAktivering).isEqualTo(forventetMinsideAktivering)
        }

        @Test
        fun `skal oppdatere deaktivert MinsideAktivering for aktør dersom den eksisterer fra før`() {
            // Arrange
            val aktør = randomAktør()
            val eksisterendeMinsideAktivering = MinsideAktivering(id = 1, aktør = aktør, aktivert = true)
            val forventetMinsideAktivering = MinsideAktivering(id = 1, aktør = aktør, aktivert = false)

            every { minsideAktiveringRepository.findByAktør(aktør) } returns eksisterendeMinsideAktivering
            every { minsideAktiveringKafkaProducer.deaktiver(aktør.aktivFødselsnummer()) } just Runs
            every { minsideAktiveringRepository.save(forventetMinsideAktivering) } returns forventetMinsideAktivering

            // Act
            val deaktiverteMinsideAktivering = minsideAktiveringService.deaktiverMinsideAktivering(aktør)

            // Assert
            assertThat(deaktiverteMinsideAktivering).isEqualTo(forventetMinsideAktivering)
        }
    }

    @Nested
    inner class HentAktiverteMinsideAktiveringerForAktører {
        @Test
        fun `skal hente alle aktiverte MinsideAktiveringer for en liste av aktører`() {
            // Arrange
            val aktør1 = randomAktør()
            val aktør2 = randomAktør()
            val aktører = listOf(aktør1, aktør2)
            val forventedeMinsideAktiveringer =
                listOf(
                    MinsideAktivering(aktør = aktør1, aktivert = true),
                    MinsideAktivering(aktør = aktør2, aktivert = true),
                )

            every { minsideAktiveringRepository.findAllByAktørInAndAktivertIsTrue(aktører) } returns forventedeMinsideAktiveringer

            // Act
            val aktiverteMinsideAktiveringer = minsideAktiveringService.hentAktiverteMinsideAktiveringerForAktører(aktører)

            // Assert
            assertThat(aktiverteMinsideAktiveringer).isEqualTo(forventedeMinsideAktiveringer)
        }
    }
}
