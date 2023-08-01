package no.nav.familie.ba.sak.integrasjoner.økonomi.oppdrag

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.felles.utbetalingsgenerator.domain.YtelseType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.springframework.stereotype.Service
import java.time.YearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType as YtelseTypeDomene

@Service
class UtbetalingsoppdragService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun genererUtbetalingsoppdragOgOppdaterTilkjentYtelse(
        behandlingId: Long,
        vedtak: Vedtak,
        forrigeBehandlingId: Long?,
        erSimulering: Boolean,
        endretMigreringsdato: YearMonth?
    ): Utbetalingsoppdrag {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandlingId)
        val nyeAndeler = tilkjentYtelse.andelerTilkjentYtelse.map { it.tilAndelData() }
        val forrigeAndeler = forrigeAndeler(forrigeBehandlingId)
        val opphørFra = opphørFra(forrigeAndeler, erSimulering, endretMigreringsdato)
        val resultat = Utbetalingsgenerator().lagUtbetalingsoppdrag(
            behandlingsinformasjon = behandlingsinformasjon(vedtak, opphørFra),
            nyeAndeler = nyeAndeler,
            forrigeAndeler = forrigeAndeler ?: emptyList(),
            sisteAndelPerKjede = sisteAndelPerKjedeForFagsak(vedtak.behandling.fagsak)
        )
        if (!erSimulering) {
            oppdaterTilkjentYtelse(tilkjentYtelse, resultat)
        }
        return resultat.utbetalingsoppdrag
    }

    private fun behandlingsinformasjon(
        vedtak: Vedtak,
        opphørFra: YearMonth?,
    ): Behandlingsinformasjon {
        val behandling = vedtak.behandling
        val fagsak = behandling.fagsak
        return Behandlingsinformasjon(
            saksbehandlerId = vedtak.endretAv,
            behandlingId = behandling.id.toString(),
            eksternBehandlingId = behandling.id,
            eksternFagsakId = fagsak.id,
            ytelse = Ytelsestype.BARNETRYGD,
            personIdent = fagsak.aktør.aktivFødselsnummer(),
            vedtaksdato = vedtak.vedtaksdato?.toLocalDate()
                ?: error("Mangler vedtaksdato fagsak=${fagsak.id} behandling=${behandling.id}"),
            opphørFra = opphørFra,
            utbetalesTil = hentUtebetalesTil(fagsak),
            erGOmregning = false,
        )
    }

    private fun hentUtebetalesTil(fagsak: Fagsak): String {
        return when (fagsak.type) {
            FagsakType.INSTITUSJON -> {
                fagsak.institusjon?.tssEksternId
                    ?: error("Fagsak ${fagsak.id} er av type institusjon og mangler informasjon om institusjonen")
            }

            else -> {
                fagsak.aktør.aktivFødselsnummer()
            }
        }
    }

    private fun opphørFra(
        nyeAndeler: List<AndelDataLongId>,
        forrigeAndeler: List<AndelDataLongId>,
        erSimulering: Boolean,
        endretMigreringsdato: YearMonth?
    ): YearMonth? {
        // TODO
        return null
    }

    private fun oppdaterTilkjentYtelse(
        tilkjentYtelse: TilkjentYtelse,
        resultat: BeregnetUtbetalingsoppdragLongId,
    ) {
        tilkjentYtelse.utbetalingsoppdrag = objectMapper.writeValueAsString(resultat.utbetalingsoppdrag)
        val andelerPåId = resultat.andeler.associateBy { it.id }
        val andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse
        val andelerSomSkalSendesTilOppdrag = andelerTilkjentYtelse.filter { it.erAndelSomSkalSendesTilOppdrag() }
        if (resultat.andeler.size != andelerSomSkalSendesTilOppdrag.size) {
            error("Forventer å ha like mange")
        }
        andelerSomSkalSendesTilOppdrag.forEach { andel ->
            val andelMedOffset = andelerPåId[andel.id]
                ?: error("Finner ikke andel med offset for andel=${andel.id}")
            andel.periodeOffset = andelMedOffset.periodeId
            andel.forrigePeriodeOffset = andelMedOffset.forrigePeriodeId
            andel.kildeBehandlingId = andelMedOffset.kildeBehandlingId
        }
        tilkjentYtelseRepository.save(tilkjentYtelse)
    }

    private fun sisteAndelPerKjedeForFagsak(fagsak: Fagsak): Map<IdentOgType, AndelDataLongId> =
        tilkjentYtelseRepository.sisteAndelPerKjedeForFagsak(fagsak.id)
            .map { IdentOgType(it.key.ident, it.key.type.tilYtelseType()) to it.value.tilAndelData() }
            .toMap()

    private fun forrigeAndeler(forrigeBehandlingId: Long?): List<AndelDataLongId>? =
        forrigeBehandlingId?.let {
            tilkjentYtelseRepository.findByBehandling(it)
        }?.andelerTilkjentYtelse?.map { it.tilAndelData() }

    private fun AndelTilkjentYtelse.tilAndelData(): AndelDataLongId =
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

    fun YtelseTypeDomene.tilYtelseType(): YtelseType = when (this) {
        YtelseTypeDomene.ORDINÆR_BARNETRYGD -> YtelseType.ORDINÆR_BARNETRYGD
        YtelseTypeDomene.UTVIDET_BARNETRYGD -> YtelseType.UTVIDET_BARNETRYGD
        YtelseTypeDomene.SMÅBARNSTILLEGG -> YtelseType.SMÅBARNSTILLEGG
    }
}
