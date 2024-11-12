package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

@Component
class UtbetalingsoppdragGenerator {
    fun lagUtbetalingsoppdrag(
        saksbehandlerId: String,
        vedtak: Vedtak,
        forrigeTilkjentYtelse: TilkjentYtelse?,
        nyTilkjentYtelse: TilkjentYtelse,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        erSimulering: Boolean,
        endretMigreringsDato: YearMonth? = null,
    ): BeregnetUtbetalingsoppdragLongId =
        Utbetalingsgenerator().lagUtbetalingsoppdrag(
            behandlingsinformasjon =
                Behandlingsinformasjon(
                    saksbehandlerId = saksbehandlerId,
                    behandlingId = vedtak.behandling.id.toString(),
                    eksternBehandlingId = vedtak.behandling.id,
                    eksternFagsakId = vedtak.behandling.fagsak.id,
                    fagsystem = FagsystemBA.BARNETRYGD,
                    personIdent =
                        vedtak.behandling.fagsak.aktør
                            .aktivFødselsnummer(),
                    vedtaksdato = vedtak.vedtaksdato?.toLocalDate() ?: LocalDate.now(),
                    opphørAlleKjederFra =
                        finnOpphørsdatoForAlleKjeder(
                            forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                            sisteAndelPerKjede = sisteAndelPerKjede,
                            endretMigreringsDato = endretMigreringsDato,
                        ),
                    utbetalesTil = hentUtebetalesTil(vedtak.behandling.fagsak),
                    // Ved simulering når migreringsdato er endret, skal vi opphøre fra den nye datoen og ikke fra første utbetaling per kjede.
                    opphørKjederFraFørsteUtbetaling = if (endretMigreringsDato != null) false else erSimulering,
                ),
            forrigeAndeler = forrigeTilkjentYtelse?.tilAndelData() ?: emptyList(),
            nyeAndeler = nyTilkjentYtelse.tilAndelData(),
            sisteAndelPerKjede = sisteAndelPerKjede.mapValues { it.value.tilAndelDataLongId() },
        )

    private fun TilkjentYtelse.tilAndelData(): List<AndelDataLongId> =
        this.andelerTilkjentYtelse.map { it.tilAndelDataLongId() }

    private fun AndelTilkjentYtelse.tilAndelDataLongId(): AndelDataLongId =
        AndelDataLongId(
            id = id,
            fom = periode.fom,
            tom = periode.tom,
            beløp = kalkulertUtbetalingsbeløp,
            personIdent = aktør.aktivFødselsnummer(),
            type = type.tilYtelseType(),
            periodeId = periodeOffset,
            forrigePeriodeId = forrigePeriodeOffset,
            kildeBehandlingId = kildeBehandlingId,
        )

    private fun finnOpphørsdatoForAlleKjeder(
        forrigeTilkjentYtelse: TilkjentYtelse?,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        endretMigreringsDato: YearMonth?,
    ): YearMonth? {
        if (forrigeTilkjentYtelse == null || sisteAndelPerKjede.isEmpty()) return null
        if (endretMigreringsDato != null) return endretMigreringsDato
        return null
    }

    private fun hentUtebetalesTil(fagsak: Fagsak): String =
        when (fagsak.type) {
            FagsakType.INSTITUSJON -> {
                fagsak.institusjon?.tssEksternId
                    ?: error("Fagsak ${fagsak.id} er av type institusjon og mangler informasjon om institusjonen")
            }

            else -> {
                fagsak.aktør.aktivFødselsnummer()
            }
        }
}
