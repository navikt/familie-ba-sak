package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.felles.utbetalingsgenerator.Utbetalingsgenerator
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Behandlingsinformasjon
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.felles.utbetalingsgenerator.domain.Fagsystem
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.YearMonth

@Component
class UtbetalingsoppdragGenerator(
    private val utbetalingsgenerator: Utbetalingsgenerator,
    private val justerUtbetalingsoppdragService: JusterUtbetalingsoppdragService,
    private val unleashNextMedContextService: UnleashNextMedContextService,
) {
    fun lagUtbetalingsoppdrag(
        saksbehandlerId: String,
        vedtak: Vedtak,
        forrigeTilkjentYtelse: TilkjentYtelse?,
        nyTilkjentYtelse: TilkjentYtelse,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        erSimulering: Boolean,
        endretMigreringsDato: YearMonth? = null,
    ): BeregnetUtbetalingsoppdragLongId {
        val beregnetUtbetalingsoppdrag =
            utbetalingsgenerator.lagUtbetalingsoppdrag(
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
        return justerUtbetalingsoppdragService.justerBeregnetUtbetalingsoppdragVedBehov(
            beregnetUtbetalingsoppdrag = beregnetUtbetalingsoppdrag,
            behandling = vedtak.behandling,
        )
    }

    private fun TilkjentYtelse.tilAndelData(): List<AndelDataLongId> =
        this.andelerTilkjentYtelse.map { it.tilAndelDataLongId() }

    private fun AndelTilkjentYtelse.tilAndelDataLongId(): AndelDataLongId {
        // Skrur på ny klassekode for enkelte fagsaker til å begynne med.
        val skalBrukeNyKlassekodeForUtvidetBarnetrygd =
            unleashNextMedContextService.isEnabled(
                toggleId = FeatureToggleConfig.SKAL_BRUKE_NY_KLASSEKODE_FOR_UTVIDET_BARNETRYGD,
                behandlingId = this.behandlingId,
            )
        return AndelDataLongId(
            id = id,
            fom = periode.fom,
            tom = periode.tom,
            beløp = kalkulertUtbetalingsbeløp,
            personIdent = aktør.aktivFødselsnummer(),
            type = type.tilYtelseType(skalBrukeNyKlassekodeForUtvidetBarnetrygd),
            periodeId = periodeOffset,
            forrigePeriodeId = forrigePeriodeOffset,
            kildeBehandlingId = kildeBehandlingId,
        )
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

enum class YtelsetypeBA(
    override val klassifisering: String,
    override val satsType: no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType = no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType.MND,
) : no.nav.familie.felles.utbetalingsgenerator.domain.Ytelsestype {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BAUTV-OP"),
    UTVIDET_BARNETRYGD_GAMMEL("BATR"),
    SMÅBARNSTILLEGG("BATRSMA"),
}

enum class FagsystemBA(
    override val kode: String,
    override val gyldigeSatstyper: Set<YtelsetypeBA>,
) : Fagsystem {
    BARNETRYGD(
        "BA",
        setOf(YtelsetypeBA.ORDINÆR_BARNETRYGD, YtelsetypeBA.UTVIDET_BARNETRYGD, YtelsetypeBA.UTVIDET_BARNETRYGD_GAMMEL, YtelsetypeBA.SMÅBARNSTILLEGG),
    ),
}

fun no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsoppdrag.tilRestUtbetalingsoppdrag(): Utbetalingsoppdrag =
    Utbetalingsoppdrag(
        kodeEndring = Utbetalingsoppdrag.KodeEndring.valueOf(this.kodeEndring.name),
        fagSystem = this.fagSystem,
        saksnummer = this.saksnummer,
        aktoer = this.aktoer,
        saksbehandlerId = this.saksbehandlerId,
        avstemmingTidspunkt = this.avstemmingTidspunkt,
        utbetalingsperiode = this.utbetalingsperiode.map { it.tilRestUtbetalingsperiode() },
        gOmregning = this.gOmregning,
    )

fun no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.tilRestUtbetalingsperiode(): Utbetalingsperiode =
    Utbetalingsperiode(
        erEndringPåEksisterendePeriode = this.erEndringPåEksisterendePeriode,
        opphør = this.opphør?.let { Opphør(it.opphørDatoFom) },
        periodeId = this.periodeId,
        forrigePeriodeId = this.forrigePeriodeId,
        datoForVedtak = this.datoForVedtak,
        klassifisering = this.klassifisering,
        vedtakdatoFom = this.vedtakdatoFom,
        vedtakdatoTom = this.vedtakdatoTom,
        sats = this.sats,
        satsType = Utbetalingsperiode.SatsType.valueOf(this.satsType.name),
        utbetalesTil = this.utbetalesTil,
        behandlingId = this.behandlingId,
        utbetalingsgrad = this.utbetalingsgrad,
    )
