package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

@Component
class BehandlingsinformasjonUtleder(
    private val endretMigreringsdatoUtleder: EndretMigreringsdatoUtleder,
    private val clockProvider: ClockProvider,
) {
    fun utled(
        saksbehandlerId: String,
        vedtak: Vedtak,
        tilkjentYtelse: TilkjentYtelse,
        forrigeTilkjentYtelse: TilkjentYtelse?,
        sisteAndelPerKjede: Map<IdentOgType, AndelDataLongId>,
        erSimulering: Boolean,
    ): Behandlingsinformasjon {
        val behandling = vedtak.behandling
        val fagsak = behandling.fagsak
        val endretMigreringsdato = endretMigreringsdatoUtleder.utled(fagsak, tilkjentYtelse, forrigeTilkjentYtelse)
        return Behandlingsinformasjon(
            saksbehandlerId = saksbehandlerId,
            behandlingId = behandling.id.toString(),
            eksternBehandlingId = behandling.id,
            eksternFagsakId = fagsak.id,
            fagsystem = FagsystemBA.BARNETRYGD,
            personIdent = fagsak.aktør.aktivFødselsnummer(),
            vedtaksdato = vedtak.vedtaksdato?.toLocalDate() ?: LocalDate.now(clockProvider.get()),
            opphørAlleKjederFra = finnOpphørsdatoForAlleKjeder(forrigeTilkjentYtelse, sisteAndelPerKjede, endretMigreringsdato),
            utbetalesTil = finnUtebetalesTil(fagsak),
            opphørKjederFraFørsteUtbetaling = finnOpphørKjederFraFørsteUtbetaling(endretMigreringsdato, erSimulering),
        )
    }

    private fun finnOpphørsdatoForAlleKjeder(
        forrigeTilkjentYtelse: TilkjentYtelse?,
        sisteAndelPerKjede: Map<IdentOgType, AndelDataLongId>,
        endretMigreringsdato: YearMonth?,
    ): YearMonth? =
        if (forrigeTilkjentYtelse == null || sisteAndelPerKjede.isEmpty()) {
            null
        } else {
            endretMigreringsdato
        }

    private fun finnUtebetalesTil(fagsak: Fagsak): String =
        when (fagsak.type) {
            FagsakType.NORMAL,
            FagsakType.BARN_ENSLIG_MINDREÅRIG,
            -> fagsak.aktør.aktivFødselsnummer()

            FagsakType.INSTITUSJON,
            -> {
                fagsak.institusjon?.tssEksternId ?: throw IllegalStateException(
                    "Fagsak ${fagsak.id} er av type institusjon og mangler informasjon om institusjonen",
                )
            }
        }

    private fun finnOpphørKjederFraFørsteUtbetaling(
        endretMigreringsdato: YearMonth?,
        erSimulering: Boolean,
    ) = if (endretMigreringsdato != null) {
        false
    } else {
        // Ved simulering når migreringsdato er endret, skal vi opphøre fra den nye datoen og ikke fra første utbetaling per kjede.
        erSimulering
    }
}
