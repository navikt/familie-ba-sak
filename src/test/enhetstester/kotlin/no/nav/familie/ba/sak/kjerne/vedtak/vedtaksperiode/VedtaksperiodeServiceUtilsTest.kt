package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.periodeErOppyltForYtelseType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class VedtaksperiodeServiceUtilsTest {

    val ytelseTyperSmåbarnstillegg =
        setOf(YtelseType.SMÅBARNSTILLEGG, YtelseType.UTVIDET_BARNETRYGD, YtelseType.ORDINÆR_BARNETRYGD)
    val ytelseTyperUtvidetOgOrdinær =
        setOf(YtelseType.UTVIDET_BARNETRYGD, YtelseType.ORDINÆR_BARNETRYGD)
    val ytelseTyperOrdinær =
        setOf(YtelseType.ORDINÆR_BARNETRYGD)

    @Test
    fun `Skal gi riktig svar for småbarnstillegg-trigger ved innvilget VedtakBegrunnelseType`() {
        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperSmåbarnstillegg,
                ytelserGjeldeneForSøkerForrigeMåned = emptyList(),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = emptyList(),
            ),
        )
    }

    @Test
    fun `Skal gi riktig svar for småbarnstillegg-trigger når VedtakBegrunnelseType er reduksjon`() {
        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.SMÅBARNSTILLEGG),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperSmåbarnstillegg,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.SMÅBARNSTILLEGG),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.ORDINÆR_BARNETRYGD),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(),
            ),
        )
    }

    @Test
    fun `Skal gi false når VedtakBegrunnelseType ikke er innvilget eller reduksjon `() {
        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.AVSLAG.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                ytelseTyperForPeriode = ytelseTyperSmåbarnstillegg,
                ytelserGjeldeneForSøkerForrigeMåned = emptyList(),
            ),
        )
    }

    @Test
    fun `Skal gi riktig svar for utvidet-trigger ved innvilget`() {
        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = emptyList(),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.INNVILGET.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = emptyList(),
            ),
        )
    }

    @Test
    fun `Skal gi riktig svar for utvidet barnetrygd-trigger når VedtakBegrunnelseType er reduksjon`() {
        Assertions.assertEquals(
            true,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.UTVIDET_BARNETRYGD),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperUtvidetOgOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.UTVIDET_BARNETRYGD),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(YtelseType.ORDINÆR_BARNETRYGD),
            ),
        )

        Assertions.assertEquals(
            false,
            VedtakBegrunnelseType.REDUKSJON.periodeErOppyltForYtelseType(
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ytelseTyperForPeriode = ytelseTyperOrdinær,
                ytelserGjeldeneForSøkerForrigeMåned = listOf(),
            ),
        )
    }
}
