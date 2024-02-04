package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
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
class UtbetalingsoppdragGenerator {
    fun lagUtbetalingsoppdrag(
        saksbehandlerId: String,
        vedtak: Vedtak,
        forrigeTilkjentYtelse: TilkjentYtelse?,
        nyTilkjentYtelse: TilkjentYtelse,
        sisteAndelPerKjede: Map<IdentOgType, AndelTilkjentYtelse>,
        erSimulering: Boolean,
        endretMigreringsDato: YearMonth? = null,
    ): BeregnetUtbetalingsoppdragLongId {
        return Utbetalingsgenerator().lagUtbetalingsoppdrag(
            behandlingsinformasjon =
                Behandlingsinformasjon(
                    saksbehandlerId = saksbehandlerId,
                    behandlingId = vedtak.behandling.id.toString(),
                    eksternBehandlingId = vedtak.behandling.id,
                    eksternFagsakId = vedtak.behandling.fagsak.id,
                    fagsystem = FagsystemBA.BARNETRYGD,
                    personIdent = vedtak.behandling.fagsak.aktør.aktivFødselsnummer(),
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
    }

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
}

abstract class AndelTilkjentYtelseForUtbetalingsoppdrag(private val andelTilkjentYtelse: AndelTilkjentYtelse) {
    val behandlingId: Long? = andelTilkjentYtelse.behandlingId
    val tilkjentYtelse: TilkjentYtelse = andelTilkjentYtelse.tilkjentYtelse
    val kalkulertUtbetalingsbeløp: Int = andelTilkjentYtelse.kalkulertUtbetalingsbeløp
    val stønadFom: YearMonth = andelTilkjentYtelse.stønadFom
    val stønadTom: YearMonth = andelTilkjentYtelse.stønadTom
    val aktør: Aktør = andelTilkjentYtelse.aktør
    val type: YtelseType = andelTilkjentYtelse.type
    abstract var periodeOffset: Long?
    abstract var forrigePeriodeOffset: Long?
    abstract var kildeBehandlingId: Long?

    override fun equals(other: Any?): Boolean {
        return if (other is AndelTilkjentYtelseForUtbetalingsoppdrag) {
            this.andelTilkjentYtelse.equals(other.andelTilkjentYtelse)
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return andelTilkjentYtelse.hashCode()
    }
}

interface AndelTilkjentYtelseForUtbetalingsoppdragFactory {
    fun pakkInnForUtbetaling(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<AndelTilkjentYtelseForUtbetalingsoppdrag>
}

class AndelTilkjentYtelseForSimuleringFactory : AndelTilkjentYtelseForUtbetalingsoppdragFactory {
    override fun pakkInnForUtbetaling(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<AndelTilkjentYtelseForUtbetalingsoppdrag> =
        andelerTilkjentYtelse.map { AndelTilkjentYtelseForSimulering(it) }

    private class AndelTilkjentYtelseForSimulering(
        andelTilkjentYtelse: AndelTilkjentYtelse,
    ) : AndelTilkjentYtelseForUtbetalingsoppdrag(andelTilkjentYtelse) {
        override var periodeOffset: Long? = andelTilkjentYtelse.periodeOffset
        override var forrigePeriodeOffset: Long? = andelTilkjentYtelse.forrigePeriodeOffset
        override var kildeBehandlingId: Long? = andelTilkjentYtelse.kildeBehandlingId
    }
}

class AndelTilkjentYtelseForIverksettingFactory : AndelTilkjentYtelseForUtbetalingsoppdragFactory {
    override fun pakkInnForUtbetaling(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<AndelTilkjentYtelseForUtbetalingsoppdrag> =
        andelerTilkjentYtelse.map { AndelTilkjentYtelseForIverksetting(it) }

    private class AndelTilkjentYtelseForIverksetting(
        private val andelTilkjentYtelse: AndelTilkjentYtelse,
    ) : AndelTilkjentYtelseForUtbetalingsoppdrag(andelTilkjentYtelse) {
        override var periodeOffset: Long?
            get() = andelTilkjentYtelse.periodeOffset
            set(value) {
                andelTilkjentYtelse.periodeOffset = value
            }

        override var forrigePeriodeOffset: Long?
            get() = andelTilkjentYtelse.forrigePeriodeOffset
            set(value) {
                andelTilkjentYtelse.forrigePeriodeOffset = value
            }

        override var kildeBehandlingId: Long?
            get() = andelTilkjentYtelse.kildeBehandlingId
            set(value) {
                andelTilkjentYtelse.kildeBehandlingId = value
            }
    }
}

fun Collection<AndelTilkjentYtelse>.pakkInnForUtbetaling(
    andelTilkjentYtelseForUtbetalingsoppdragFactory: AndelTilkjentYtelseForUtbetalingsoppdragFactory,
) = andelTilkjentYtelseForUtbetalingsoppdragFactory.pakkInnForUtbetaling(this)

enum class YtelsetypeBA(
    override val klassifisering: String,
    override val satsType: no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType = no.nav.familie.felles.utbetalingsgenerator.domain.Utbetalingsperiode.SatsType.MND,
) : no.nav.familie.felles.utbetalingsgenerator.domain.Ytelsestype {
    ORDINÆR_BARNETRYGD("BATR"),
    UTVIDET_BARNETRYGD("BATR"),
    SMÅBARNSTILLEGG("BATRSMA"),
}

enum class FagsystemBA(
    override val kode: String,
    override val gyldigeSatstyper: Set<YtelsetypeBA>,
) : Fagsystem {
    BARNETRYGD(
        "BA",
        setOf(YtelsetypeBA.ORDINÆR_BARNETRYGD, YtelsetypeBA.UTVIDET_BARNETRYGD, YtelsetypeBA.SMÅBARNSTILLEGG),
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
