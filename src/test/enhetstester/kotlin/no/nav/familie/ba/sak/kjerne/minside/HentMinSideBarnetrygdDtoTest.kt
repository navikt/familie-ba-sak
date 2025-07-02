package no.nav.familie.ba.sak.kjerne.minside

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

class HentMinSideBarnetrygdDtoTest {
    @Nested
    inner class FraDomene {
        @Test
        fun `skal opprette dto fra domeneobjektet med både ordinær og utvidet`() {
            // Arrange
            val minSideBarnetrygd =
                MinSideBarnetrygd(
                    ordinær = MinSideBarnetrygd.Ordinær(YearMonth.of(2025, 1)),
                    utvidet = MinSideBarnetrygd.Utvidet(YearMonth.of(2025, 3)),
                )

            // Act
            val dto = HentMinSideBarnetrygdDto.Suksess.opprettFraDomene(minSideBarnetrygd)

            // Assert
            assertThat(dto?.barnetrygd).isEqualTo(minSideBarnetrygd)
        }

        @Test
        fun `skal opprette dto fra domeneobjektet med uten ordinær eller utvidet`() {
            // Arrange
            val minSideBarnetrygd =
                MinSideBarnetrygd(
                    ordinær = null,
                    utvidet = null,
                )

            // Act
            val dto = HentMinSideBarnetrygdDto.Suksess.opprettFraDomene(minSideBarnetrygd)

            // Assert
            assertThat(dto?.barnetrygd?.ordinær).isNull()
            assertThat(dto?.barnetrygd?.utvidet).isNull()
        }

        @Test
        fun `skal opprette dto fra domene objektet hvis domeneobjektet er null`() {
            // Act
            val dto = HentMinSideBarnetrygdDto.Suksess.opprettFraDomene(null)

            // Assert
            assertThat(dto).isNull()
        }
    }
}
