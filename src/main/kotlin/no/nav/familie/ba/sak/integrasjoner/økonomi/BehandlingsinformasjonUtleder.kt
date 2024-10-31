package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

@Component
class BehandlingsinformasjonUtleder(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val behandlingService: BehandlingService,
) {
    fun utled(
        saksbehandlerId: String,
        vedtak: Vedtak,
        forrigeTilkjentYtelse: TilkjentYtelse?,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        erSimulering: Boolean,
    ): Behandlingsinformasjon {
        val endretMigreringsDato =
            beregnOmMigreringsDatoErEndret(
                vedtak.behandling,
                forrigeTilkjentYtelse?.andelerTilkjentYtelse?.minOfOrNull { it.stønadFom },
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
        )
    }

    private fun beregnOmMigreringsDatoErEndret(
        behandling: Behandling,
        forrigeTilstandFraDato: YearMonth?,
    ): YearMonth? {
        val erMigrertSak =
            behandlingHentOgPersisterService
                .hentBehandlinger(behandling.fagsak.id)
                .any { it.type == BehandlingType.MIGRERING_FRA_INFOTRYGD }

        if (!erMigrertSak) {
            return null
        }

        val nyttTilstandFraDato =
            behandlingService
                .hentMigreringsdatoPåFagsak(fagsakId = behandling.fagsak.id)
                ?.toYearMonth()
                ?.plusMonths(1)

        return if (forrigeTilstandFraDato != null &&
            nyttTilstandFraDato != null &&
            forrigeTilstandFraDato.isAfter(nyttTilstandFraDato)
        ) {
            nyttTilstandFraDato
        } else {
            null
        }
    }

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
