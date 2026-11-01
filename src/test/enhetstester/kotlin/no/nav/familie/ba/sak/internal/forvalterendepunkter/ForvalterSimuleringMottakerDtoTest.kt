package no.nav.familie.ba.sak.internal.forvalterendepunkter

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ForvalterSimuleringMottakerDtoTest {
    @Test
    fun `skal mappe mottaker og posteringer til dto med behandlingId i stedet for behandling`() {
        // Arrange
        val behandling = lagBehandling(id = 42)
        val postering =
            lagØkonomiSimuleringPostering(
                beløp = -1054,
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.of(2026, 1, 1),
                tom = LocalDate.of(2026, 1, 31),
                posteringType = PosteringType.FEILUTBETALING,
                forfallsdato = LocalDate.of(2026, 2, 20),
                utenInntrekk = true,
                fagsakId = 7,
            )
        val mottaker =
            lagØkonomiSimuleringMottaker(
                id = 1,
                mottakerNummer = "12345678910",
                mottakerType = MottakerType.BRUKER,
                behandling = behandling,
                økonomiSimuleringPostering = listOf(postering),
            )

        // Act
        val dto = mottaker.tilForvalterSimuleringMottakerDto()

        // Assert
        assertThat(dto.id).isEqualTo(1)
        assertThat(dto.behandlingId).isEqualTo(42)
        assertThat(dto.mottakerNummer).isEqualTo("12345678910")
        assertThat(dto.mottakerType).isEqualTo(MottakerType.BRUKER)
        assertThat(dto.posteringer).hasSize(1)
        val posteringDto = dto.posteringer.single()
        assertThat(posteringDto.fagOmrådeKode).isEqualTo(FagOmrådeKode.BARNETRYGD)
        assertThat(posteringDto.fom).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(posteringDto.tom).isEqualTo(LocalDate.of(2026, 1, 31))
        assertThat(posteringDto.betalingType).isEqualTo(BetalingType.KREDIT)
        assertThat(posteringDto.beløp).isEqualByComparingTo("-1054")
        assertThat(posteringDto.posteringType).isEqualTo(PosteringType.FEILUTBETALING)
        assertThat(posteringDto.forfallsdato).isEqualTo(LocalDate.of(2026, 2, 20))
        assertThat(posteringDto.utenInntrekk).isTrue()
        assertThat(posteringDto.fagsakId).isEqualTo(7)
    }

    @Test
    fun `skal ikke ta med mottakerNummer i toString`() {
        // Arrange
        val dto = lagØkonomiSimuleringMottaker(mottakerNummer = "12345678910", behandling = lagBehandling(id = 42)).tilForvalterSimuleringMottakerDto()

        // Act
        val tekst = dto.toString()

        // Assert
        assertThat(tekst).doesNotContain("12345678910")
        assertThat(tekst).contains("behandlingId=42")
    }
}
