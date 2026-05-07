package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.fagsaklåsing.FagsakLåsHendelse
import no.nav.familie.ba.sak.kjerne.fagsaklåsing.FagsakLåsing
import no.nav.familie.ba.sak.kjerne.fagsaklåsing.FagsakLåsingRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class FagsakLåsingRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val fagsakLåsingRepository: FagsakLåsingRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class FinnAktivLåsForFagsak {
        @Test
        fun `skal returnere aktiv lås for fagsaken`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = FagsakStatus.LÅST))
            val låstTidspunkt = LocalDateTime.of(2025, 6, 15, 10, 30, 0)

            fagsakLåsingRepository.save(
                FagsakLåsing(
                    fagsak = fagsak,
                    tidspunkt = låstTidspunkt,
                    hendelse = FagsakLåsHendelse.LÅST,
                    begrunnelse = "Låst etter 1 år det eldste barnet ble 18.",
                    aktiv = true,
                ),
            )

            // Act
            val gjeldendeLås = fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id)

            // Assert
            assertThat(gjeldendeLås).isNotNull
            assertThat(gjeldendeLås!!.tidspunkt).isEqualTo(låstTidspunkt)
            assertThat(gjeldendeLås.hendelse).isEqualTo(FagsakLåsHendelse.LÅST)
            assertThat(gjeldendeLås.aktiv).isTrue()
        }

        @Test
        fun `skal returnere null når fagsaken ikke har noen aktiv lås`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = FagsakStatus.OPPRETTET))

            // Act
            val gjeldendeLås = fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id)

            // Assert
            assertThat(gjeldendeLås).isNull()
        }

        @Test
        fun `skal ikke returnere inaktiv lås`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør, status = FagsakStatus.AVSLUTTET))

            fagsakLåsingRepository.save(
                FagsakLåsing(
                    fagsak = fagsak,
                    tidspunkt = LocalDateTime.of(2025, 6, 15, 10, 30, 0),
                    hendelse = FagsakLåsHendelse.LÅST,
                    begrunnelse = "Låst etter 1 år det eldste barnet ble 18.",
                    aktiv = false,
                ),
            )

            // Act
            val gjeldendeLås = fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id)

            // Assert
            assertThat(gjeldendeLås).isNull()
        }
    }
}
