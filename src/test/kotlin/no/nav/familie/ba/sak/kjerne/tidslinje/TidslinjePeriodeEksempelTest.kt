package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.fpsak.tidsserie.LocalDateTimeline
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidslinjePeriodeEksempelTest {
    @Test
    fun `Skal slå sammen barn sine oppfylte vilkårsperioder`() {
        val borMedSøker2018 = TidslinjeEksempel(
            perioder = listOf(
                PeriodeEksempel(
                    fom = LocalDate.of(2018, 1, 1),
                    tom = LocalDate.of(2018, 12, 31),
                    data = Vilkår.BOR_MED_SØKER
                )
            ),
            tema = TidslinjeTema.VILKÅR_PERIODER_BARN
        )

        val bosattIRiket2018 = TidslinjeEksempel(
            perioder = listOf(
                PeriodeEksempel(
                    fom = LocalDate.of(2018, 1, 1),
                    tom = LocalDate.of(2018, 12, 31),
                    data = Vilkår.BOSATT_I_RIKET
                )
            ),
            tema = TidslinjeTema.VILKÅR_PERIODER_BARN
        )

        val lovligOpphold2018 = TidslinjeEksempel(
            perioder = listOf(
                PeriodeEksempel(
                    fom = LocalDate.of(2018, 1, 1),
                    tom = LocalDate.of(2018, 12, 31),
                    data = Vilkår.LOVLIG_OPPHOLD
                )
            ),
            tema = TidslinjeTema.VILKÅR_PERIODER_BARN
        )

        val under18ÅrJuni2018Plus18År = TidslinjeEksempel(
            perioder = listOf(
                PeriodeEksempel(
                    fom = LocalDate.of(2018, 6, 12),
                    tom = LocalDate.of(2018, 6, 12).plusYears(18),
                    data = Vilkår.UNDER_18_ÅR
                )
            ),
            tema = TidslinjeTema.VILKÅR_PERIODER_BARN
        )

        val giftPartnerskap2018 = TidslinjeEksempel(
            perioder = listOf(
                PeriodeEksempel(
                    fom = LocalDate.of(2018, 1, 1),
                    tom = LocalDate.of(2018, 12, 31),
                    data = Vilkår.GIFT_PARTNERSKAP
                )
            ),
            tema = TidslinjeTema.VILKÅR_PERIODER_BARN
        )

        val localDateTimelineBarn: LocalDateTimeline<List<Vilkår>> = listOf(
            borMedSøker2018,
            bosattIRiket2018,
            lovligOpphold2018,
            under18ÅrJuni2018Plus18År,
            giftPartnerskap2018
        ).map { it.tilLocalDateTimeline() }.kombinerTidslinjer()

        localDateTimelineBarn.toSegments().forEach { println(it) }
    }
}
