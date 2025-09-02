package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.datagenerator.lagGrMatrikkelOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrUtenlandskOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseOppholdsadresse
import no.nav.familie.kontrakter.ba.svalbardtillegg.SvalbardKommune
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RestManglendeSvalbardmerkingPeriodeTest {
    @Nested
    inner class TilSvalbardOppholdTidslinje {
        @Test
        fun `skal lage perioder med tom dato satt til fom dato til neste element dersom tom dato ikke finnes`() {
            // Arrange
            val førsteFom = LocalDate.of(2021, 1, 1)
            val andreFom = LocalDate.of(2022, 5, 5)
            val tredjeFom = LocalDate.of(2023, 10, 10)

            val grOppholdsadresser =
                listOf(
                    lagGrVegadresseOppholdsadresse(
                        kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                        periode = DatoIntervallEntitet(fom = førsteFom, tom = null),
                    ),
                    lagGrMatrikkelOppholdsadresse(
                        kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                        periode = DatoIntervallEntitet(fom = andreFom, tom = null),
                    ),
                    lagGrUtenlandskOppholdsadresse(
                        oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
                        periode = DatoIntervallEntitet(fom = tredjeFom, tom = null),
                    ),
                )

            // Act
            val oppholdsadresseTidslinje = grOppholdsadresser.tilSvalbardOppholdTidslinje()

            // Assert
            val perioder = oppholdsadresseTidslinje.tilPerioder()
            assertThat(perioder).hasSize(3)

            assertThat(perioder[0].fom).isEqualTo(førsteFom)
            assertThat(perioder[0].tom).isEqualTo(andreFom.minusDays(1))

            assertThat(perioder[1].fom).isEqualTo(andreFom)
            assertThat(perioder[1].tom).isEqualTo(tredjeFom.minusDays(1))

            assertThat(perioder[2].fom).isEqualTo(tredjeFom)
            assertThat(perioder[2].tom).isNull()
        }

        @Test
        fun `skal lage perioder med tom dato dersom tom dato finnes`() {
            // Arrange
            val førsteFom = LocalDate.of(2021, 1, 1)
            val førsteTom = LocalDate.of(2022, 5, 4)
            val andreFom = LocalDate.of(2022, 5, 5)
            val andreTom = LocalDate.of(2023, 10, 9)
            val tredjeFom = LocalDate.of(2023, 10, 10)

            val grOppholdsadresser =
                listOf(
                    lagGrVegadresseOppholdsadresse(
                        kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                        periode = DatoIntervallEntitet(fom = førsteFom, tom = førsteTom),
                    ),
                    lagGrMatrikkelOppholdsadresse(
                        kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                        periode = DatoIntervallEntitet(fom = andreFom, tom = andreTom),
                    ),
                    lagGrUtenlandskOppholdsadresse(
                        oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
                        periode = DatoIntervallEntitet(fom = tredjeFom, tom = null),
                    ),
                )

            // Act
            val oppholdsadresseTidslinje = grOppholdsadresser.tilSvalbardOppholdTidslinje()

            // Assert
            val perioder = oppholdsadresseTidslinje.tilPerioder()
            assertThat(perioder).hasSize(3)

            assertThat(perioder[0].fom).isEqualTo(førsteFom)
            assertThat(perioder[0].tom).isEqualTo(førsteTom)

            assertThat(perioder[1].fom).isEqualTo(andreFom)
            assertThat(perioder[1].tom).isEqualTo(andreTom)

            assertThat(perioder[2].fom).isEqualTo(tredjeFom)
            assertThat(perioder[2].tom).isNull()
        }
    }
}
