package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

@Component
class BehandlingsinformasjonUtleder(
    private val endretMigreringsdatoUtleder: EndretMigreringsdatoUtleder,
    private val clock: Clock
) {
    fun utled(
        saksbehandlerId: String,
        vedtak: Vedtak,
        forrigeTilkjentYtelse: TilkjentYtelse?,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        erSimulering: Boolean,
    ): Behandlingsinformasjon {
        val endretMigreringsDato =
            endretMigreringsdatoUtleder.utled(
                vedtak.behandling.fagsak,
                forrigeTilkjentYtelse,
            )
        return Behandlingsinformasjon(
            saksbehandlerId = saksbehandlerId,
            behandlingId = vedtak.behandling.id.toString(),
            eksternBehandlingId = vedtak.behandling.id,
            eksternFagsakId = vedtak.behandling.fagsak.id,
            fagsystem = FagsystemBA.BARNETRYGD,
            personIdent =
                vedtak.behandling.fagsak.aktør
                    .aktivFødselsnummer(),
            vedtaksdato = vedtak.vedtaksdato?.toLocalDate() ?: LocalDate.now(clock),
            opphørAlleKjederFra =
                finnOpphørsdatoForAlleKjeder(
                    forrigeTilkjentYtelse = forrigeTilkjentYtelse,
                    sisteAndelPerKjede = sisteAndelPerKjede,
                    endretMigreringsDato = endretMigreringsDato,
                ),
            utbetalesTil = hentUtebetalesTil(vedtak.behandling.fagsak),
            // Ved simulering når migreringsdato er endret, skal vi opphøre fra den nye datoen og ikke fra første utbetaling per kjede.
            opphørKjederFraFørsteUtbetaling = finnOpphørKjederFraFørsteUtbetaling(endretMigreringsDato, erSimulering),
        )
    }

    private fun finnOpphørsdatoForAlleKjeder(
        forrigeTilkjentYtelse: TilkjentYtelse?,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        endretMigreringsDato: YearMonth?,
    ): YearMonth? {
        if (forrigeTilkjentYtelse == null || sisteAndelPerKjede.isEmpty()) {
            return null
        }
        if (endretMigreringsDato != null) {
            return endretMigreringsDato
        }
        return null
    }

    private fun hentUtebetalesTil(fagsak: Fagsak): String =
        when (fagsak.type) {
            FagsakType.INSTITUSJON,
            -> {
                fagsak.institusjon?.tssEksternId
                    ?: error("Fagsak ${fagsak.id} er av type institusjon og mangler informasjon om institusjonen")
            }

            FagsakType.NORMAL,
            FagsakType.BARN_ENSLIG_MINDREÅRIG,
            -> fagsak.aktør.aktivFødselsnummer()
        }

    private fun finnOpphørKjederFraFørsteUtbetaling(
        endretMigreringsDato: YearMonth?,
        erSimulering: Boolean,
    ) =
        if (endretMigreringsDato != null) {
            false
        } else {
            erSimulering
        }
}
