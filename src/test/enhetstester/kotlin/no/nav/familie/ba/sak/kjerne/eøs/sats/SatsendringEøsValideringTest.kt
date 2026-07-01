package no.nav.familie.ba.sak.kjerne.eøs.sats

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsSvar
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.Intervall
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtfyltUtenlandskPeriodebeløp
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class SatsendringEøsValideringTest {
    private val land = "PL"
    private val forrigeSats =
        EøsSats(
            land = land,
            valuta = "PLN",
            beløp = BigDecimal("1000"),
            fom = YearMonth.of(2025, 1),
            tom = YearMonth.of(2025, 12),
            intervall = Intervall.MÅNEDLIG,
        )
    private val nySats =
        EøsSats(
            land = land,
            valuta = "PLN",
            beløp = BigDecimal("1200"),
            fom = YearMonth.of(2026, 1),
            intervall = Intervall.MÅNEDLIG,
        )

    private fun lagUtfyltUtenlandskPeriodebeløp(
        beløp: BigDecimal = forrigeSats.beløp,
        valutakode: String = nySats.valuta,
        intervall: Intervall = nySats.intervall,
    ) = UtfyltUtenlandskPeriodebeløp(
        id = 1L,
        behandlingId = 42L,
        fom = YearMonth.of(2025, 1),
        tom = null,
        barnAktører = setOf(lagAktør(randomFnr())),
        beløp = beløp,
        valutakode = valutakode,
        intervall = intervall,
        utbetalingsland = land,
        kalkulertMånedligBeløp = beløp,
    )

    @Test
    fun `kaster ikke feil når utenlandsk periodebeløp samsvarer med forrige og ny sats`() {
        // Arrange
        val utenlandskPeriodebeløp = lagUtfyltUtenlandskPeriodebeløp()

        // Act & Assert
        assertThatCode {
            SatsendringEøsValidering.validerAtUtenlandskPeriodebeløpKanOppdateresAutomatisk(
                utenlandskPeriodebeløp = utenlandskPeriodebeløp,
                forrigeSats = forrigeSats,
                nySats = nySats,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `kaster feil ved avvik i beløp mot forrige sats`() {
        // Arrange
        val utenlandskPeriodebeløp = lagUtfyltUtenlandskPeriodebeløp(beløp = BigDecimal("500"))

        // Act & Assert
        assertThatThrownBy {
            SatsendringEøsValidering.validerAtUtenlandskPeriodebeløpKanOppdateresAutomatisk(
                utenlandskPeriodebeløp = utenlandskPeriodebeløp,
                forrigeSats = forrigeSats,
                nySats = nySats,
            )
        }.isInstanceOf(AutovedtakMåBehandlesManueltFeil::class.java)
            .hasMessage(SatsendringEøsSvar.SATSENDRING_EØS_MÅ_BEHANDLES_MANUELT.melding)
    }

    @Test
    fun `kaster feil ved avvik i valuta mot ny sats`() {
        // Arrange
        val utenlandskPeriodebeløp = lagUtfyltUtenlandskPeriodebeløp(valutakode = "EUR")

        // Act & Assert
        assertThatThrownBy {
            SatsendringEøsValidering.validerAtUtenlandskPeriodebeløpKanOppdateresAutomatisk(
                utenlandskPeriodebeløp = utenlandskPeriodebeløp,
                forrigeSats = forrigeSats,
                nySats = nySats,
            )
        }.isInstanceOf(AutovedtakMåBehandlesManueltFeil::class.java)
            .hasMessage(SatsendringEøsSvar.SATSENDRING_EØS_MÅ_BEHANDLES_MANUELT.melding)
    }

    @Test
    fun `kaster feil ved avvik i intervall mot ny sats`() {
        // Arrange
        val utenlandskPeriodebeløp = lagUtfyltUtenlandskPeriodebeløp(intervall = Intervall.ÅRLIG)

        // Act & Assert
        assertThatThrownBy {
            SatsendringEøsValidering.validerAtUtenlandskPeriodebeløpKanOppdateresAutomatisk(
                utenlandskPeriodebeløp = utenlandskPeriodebeløp,
                forrigeSats = forrigeSats,
                nySats = nySats,
            )
        }.isInstanceOf(AutovedtakMåBehandlesManueltFeil::class.java)
            .hasMessage(SatsendringEøsSvar.SATSENDRING_EØS_MÅ_BEHANDLES_MANUELT.melding)
    }
}
