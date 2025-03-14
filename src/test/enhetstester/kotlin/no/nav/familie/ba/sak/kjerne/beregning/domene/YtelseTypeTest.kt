package no.nav.familie.ba.sak.kjerne.beregning.domene

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.YtelsetypeBA
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class YtelseTypeTest {
    @Nested
    inner class TilYtelseTypeTest {
        @Test
        fun `skal mappe ORDINÆR_BARNETRYGD ytelsestype til ORDINÆR_BARNETRYGD ytelsetypeBA`() {
            // Act
            val ytelsetypeBA = YtelseType.ORDINÆR_BARNETRYGD.tilYtelseType()

            // Assert
            assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.ORDINÆR_BARNETRYGD)
        }

        @Test
        fun `skal mappe UTVIDET_BARNETRYGD ytelsestype til UTVIDET_BARNETRYGD ytelsetypeBA`() {
            // Act
            val ytelsetypeBA = YtelseType.UTVIDET_BARNETRYGD.tilYtelseType()

            // Assert
            assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.UTVIDET_BARNETRYGD)
        }

        @Test
        fun `skal mappe SMÅBARNSTILLEGG ytelsestype til SMÅBARNSTILLEGG ytelsetypeBA`() {
            // Act
            val ytelsetypeBA = YtelseType.SMÅBARNSTILLEGG.tilYtelseType()

            // Assert
            assertThat(ytelsetypeBA).isEqualTo(YtelsetypeBA.SMÅBARNSTILLEGG)
        }
    }

    @Nested
    inner class TilSatsTypeTest {
        @Test
        fun `tilSatsType for ORDINÆR_BARNETRYGD for en som er under 6 år i august 2024 skal returnere TILLEGGS_ORBA`() {
            val person = lagPerson(fødselsdato = LocalDate.of(2024, 8, 1).minusYears(6).plusMonths(1))
            val ytelseDato = LocalDate.of(2024, 8, 1)

            val result = YtelseType.ORDINÆR_BARNETRYGD.tilSatsType(person, ytelseDato)
            assertThat(result).isEqualTo(SatsType.TILLEGG_ORBA)
        }

        @Test
        fun `tilSatsType for ORDINÆR_BARNETRYGD for en som er under 6 år i september 2024 skal returnere ORBA`() {
            val person = lagPerson(fødselsdato = LocalDate.of(2024, 9, 1).minusYears(6).plusMonths(1))
            val ytelseDato = LocalDate.of(2024, 9, 1)

            val result = YtelseType.ORDINÆR_BARNETRYGD.tilSatsType(person, ytelseDato)
            assertThat(result).isEqualTo(SatsType.ORBA)
        }

        @Test
        fun `tilSatsType for ORDINÆR_BARNETRYGD etter 6 år`() {
            val person = mockk<Person>()
            val ytelseDato = LocalDate.of(2026, 1, 1)
            every { person.hentSeksårsdag() } returns LocalDate.of(2025, 1, 1)

            val result = YtelseType.ORDINÆR_BARNETRYGD.tilSatsType(person, ytelseDato)
            assertThat(result).isEqualTo(SatsType.ORBA)
        }

        @Test
        fun `tilSatsType for UTVIDET_BARNETRYGD`() {
            val person = mockk<Person>()
            val ytelseDato = LocalDate.of(2020, 1, 1)

            val result = YtelseType.UTVIDET_BARNETRYGD.tilSatsType(person, ytelseDato)
            assertThat(result).isEqualTo(SatsType.UTVIDET_BARNETRYGD)
        }

        @Test
        fun `tilSatsType for SMÅBARNSTILLEGG`() {
            val person = mockk<Person>()
            val ytelseDato = LocalDate.of(2020, 1, 1)

            val result = YtelseType.SMÅBARNSTILLEGG.tilSatsType(person, ytelseDato)
            assertThat(result).isEqualTo(SatsType.SMA)
        }
    }
}
