package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import java.time.YearMonth

data class IdentOgType(
    val ident: String,
    val type: YtelseType
)

object ØkonomiUtils {

    /**
     * Deler andeler inn i gruppene de skal kjedes i. Utbetalingsperioder kobles i kjeder per person, bortsett fra
     * småbarnstillegg og utvidet barnetrygd som separeres i to kjeder for søker. På grunn av dette legges et suffix
     * på småbarnstillegg når vi arbeider med map.
     *
     * @param[andelerForInndeling] andeler som skal sorteres i grupper for kjeding
     * @return ident med kjedegruppe.
     */
    fun kjedeinndelteAndeler(andelerForInndeling: List<AndelTilkjentYtelseForUtbetalingsoppdrag>): Map<IdentOgType, List<AndelTilkjentYtelseForUtbetalingsoppdrag>> {
        val andeler = andelerForInndeling.groupBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type) }
        val småbarnstillegg = andeler.filter { it.key.type == YtelseType.SMÅBARNSTILLEGG }


        if (småbarnstillegg.size > 1) {
            throw IllegalArgumentException("Finnes flere personer med småbarnstillegg")
        }
        return andeler
    }

    /**
     * Lager oversikt over siste andel i hver kjede som finnes uten endring i oppdatert tilstand.
     * Vi må opphøre og eventuelt gjenoppbygge hver kjede etter denne. Må ta vare på andel og ikke kun offset da
     * filtrering av oppdaterte andeler senere skjer før offset blir satt.
     * Personident er identifikator for hver kjede, med unntak av småbarnstillegg som vil være en egen "person".
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[oppdaterteKjeder] nåværende tilstand
     * @return map med personident og siste bestående andel. Bestående andel=null dersom alle opphøres eller ny person.
     */
    fun sisteBeståendeAndelPerKjede(
        forrigeKjeder: Map<IdentOgType, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        oppdaterteKjeder: Map<IdentOgType, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>
    ): Map<IdentOgType, AndelTilkjentYtelseForUtbetalingsoppdrag?> {
        val allePersoner = forrigeKjeder.keys.union(oppdaterteKjeder.keys)
        return allePersoner.associateWith { kjedeIdentifikator ->
            beståendeAndelerIKjede(
                forrigeKjede = forrigeKjeder[kjedeIdentifikator],
                oppdatertKjede = oppdaterteKjeder[kjedeIdentifikator]
            )
                ?.sortedBy { it.periodeOffset }?.lastOrNull()
        }
    }

    /**
     * Finn alle presidenter i forrige og oppdatert liste. Presidentene er identifikatorn for hver kjede.
     * Set andeler tilkjentytelse til null som indikerer at hele kjeden skal opphøre.
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[oppdaterteKjeder] nåværende tilstand
     * @return map med personident og andel=null som markerer at alle andeler skal opphøres.
     */
    fun sisteAndelPerKjede(
        forrigeKjeder: Map<IdentOgType, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        oppdaterteKjeder: Map<IdentOgType, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>
    ): Map<IdentOgType, AndelTilkjentYtelseForUtbetalingsoppdrag?> =
        forrigeKjeder.keys.union(oppdaterteKjeder.keys).associateWith { null }

    private fun beståendeAndelerIKjede(
        forrigeKjede: List<AndelTilkjentYtelseForUtbetalingsoppdrag>?,
        oppdatertKjede: List<AndelTilkjentYtelseForUtbetalingsoppdrag>?
    ): List<AndelTilkjentYtelseForUtbetalingsoppdrag>? {
        val forrige = forrigeKjede?.toSet() ?: emptySet()
        val oppdatert = oppdatertKjede?.toSet() ?: emptySet()
        val førsteEndring = forrige.disjunkteAndeler(oppdatert).minByOrNull { it.stønadFom }?.stønadFom
        return if (førsteEndring != null) {
            forrige.snittAndeler(oppdatert)
                .filter { it.stønadFom.isBefore(førsteEndring) }
        } else {
            forrigeKjede ?: emptyList()
        }
    }

    /**
     * Setter eksisterende offset og kilde på andeler som skal bestå
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[oppdaterteKjeder] nåværende tilstand
     * @return map med personident og oppdaterte kjeder
     */
    fun oppdaterBeståendeAndelerMedOffset(
        oppdaterteKjeder: Map<IdentOgType, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        forrigeKjeder: Map<IdentOgType, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>
    ): Map<IdentOgType, List<AndelTilkjentYtelseForUtbetalingsoppdrag>> {
        oppdaterteKjeder
            .filter { forrigeKjeder.containsKey(it.key) }
            .forEach { (kjedeIdentifikator, oppdatertKjede) ->
                val beståendeFraForrige =
                    beståendeAndelerIKjede(
                        forrigeKjede = forrigeKjeder.getValue(kjedeIdentifikator),
                        oppdatertKjede = oppdatertKjede
                    )
                beståendeFraForrige?.forEach { bestående ->
                    val beståendeIOppdatert = oppdatertKjede.find { it.erTilsvarendeForUtbetaling(bestående) }
                        ?: error("Kan ikke finne andel fra utledet bestående andeler i oppdatert tilstand.")
                    beståendeIOppdatert.periodeOffset = bestående.periodeOffset
                    beståendeIOppdatert.forrigePeriodeOffset = bestående.forrigePeriodeOffset
                    beståendeIOppdatert.kildeBehandlingId = bestående.kildeBehandlingId
                }
            }
        return oppdaterteKjeder
    }

    /**
     * Tar utgangspunkt i ny tilstand og finner andeler som må bygges opp (nye, endrede og bestående etter første endring)
     *
     * @param[oppdaterteKjeder] ny tilstand
     * @param[sisteBeståendeAndelIHverKjede] andeler man må bygge opp etter
     * @return andeler som må bygges fordelt på kjeder
     */
    fun andelerTilOpprettelse(
        oppdaterteKjeder: Map<IdentOgType, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        sisteBeståendeAndelIHverKjede: Map<IdentOgType, AndelTilkjentYtelseForUtbetalingsoppdrag?>
    ): List<List<AndelTilkjentYtelseForUtbetalingsoppdrag>> =
        oppdaterteKjeder.map { (kjedeIdentifikator, oppdatertKjedeTilstand) ->
            if (sisteBeståendeAndelIHverKjede[kjedeIdentifikator] != null) {
                oppdatertKjedeTilstand.filter { it.stønadFom.isAfter(sisteBeståendeAndelIHverKjede[kjedeIdentifikator]!!.stønadTom) }
            } else {
                oppdatertKjedeTilstand
            }
        }.filter { it.isNotEmpty() }

    /**
     * Tar utgangspunkt i forrige tilstand og finner kjeder med andeler til opphør og tilhørende opphørsdato
     *
     * @param[forrigeKjeder] ny tilstand
     * @param[sisteBeståendeAndelIHverKjede] andeler man må bygge opp etter
     * @param[endretMigreringsDato] Satt betyr at opphørsdato skal settes fra før tidligeste dato i eksisterende kjede.
     * @return map av siste andel og opphørsdato fra kjeder med opphør
     */
    fun andelerTilOpphørMedDato(
        forrigeKjeder: Map<IdentOgType, List<AndelTilkjentYtelseForUtbetalingsoppdrag>>,
        sisteBeståendeAndelIHverKjede: Map<IdentOgType, AndelTilkjentYtelseForUtbetalingsoppdrag?>,
        endretMigreringsDato: YearMonth? = null
    ): List<Pair<AndelTilkjentYtelseForUtbetalingsoppdrag, YearMonth>> =
        forrigeKjeder
            .mapValues { (person, forrigeAndeler) ->
                forrigeAndeler.filter {
                    altIKjedeOpphøres(person, sisteBeståendeAndelIHverKjede) ||
                        andelOpphøres(person, it, sisteBeståendeAndelIHverKjede)
                }
            }
            .filter { (_, andelerSomOpphøres) -> andelerSomOpphøres.isNotEmpty() }
            .mapValues { andelForKjede -> andelForKjede.value.sortedBy { it.stønadFom } }
            .map { (_, kjedeEtterFørsteEndring) ->
                kjedeEtterFørsteEndring.last() to (
                    endretMigreringsDato
                        ?: kjedeEtterFørsteEndring.first().stønadFom
                    )
            }

    private fun altIKjedeOpphøres(
        kjedeidentifikator: IdentOgType,
        sisteBeståendeAndelIHverKjede: Map<IdentOgType, AndelTilkjentYtelseForUtbetalingsoppdrag?>
    ): Boolean = sisteBeståendeAndelIHverKjede[kjedeidentifikator] == null

    private fun andelOpphøres(
        kjedeidentifikator: IdentOgType,
        andel: AndelTilkjentYtelseForUtbetalingsoppdrag,
        sisteBeståendeAndelIHverKjede: Map<IdentOgType, AndelTilkjentYtelseForUtbetalingsoppdrag?>
    ): Boolean = andel.stønadFom > sisteBeståendeAndelIHverKjede[kjedeidentifikator]!!.stønadTom

    const val SMÅBARNSTILLEGG_SUFFIX = "_SMÅBARNSTILLEGG"
}

/**
 * Merk at det søkes snitt på visse attributter (erTilsvarendeForUtbetaling)
 * og man kun returnerer objekter fra receiver (ikke other)
 */
private fun Set<AndelTilkjentYtelseForUtbetalingsoppdrag>.snittAndeler(other: Set<AndelTilkjentYtelseForUtbetalingsoppdrag>): Set<AndelTilkjentYtelseForUtbetalingsoppdrag> {
    val andelerKunIDenne = this.subtractAndeler(other)
    return this.subtractAndeler(andelerKunIDenne)
}

private fun Set<AndelTilkjentYtelseForUtbetalingsoppdrag>.disjunkteAndeler(other: Set<AndelTilkjentYtelseForUtbetalingsoppdrag>): Set<AndelTilkjentYtelseForUtbetalingsoppdrag> {
    val andelerKunIDenne = this.subtractAndeler(other)
    val andelerKunIAnnen = other.subtractAndeler(this)
    return andelerKunIDenne.union(andelerKunIAnnen)
}

private fun Set<AndelTilkjentYtelseForUtbetalingsoppdrag>.subtractAndeler(other: Set<AndelTilkjentYtelseForUtbetalingsoppdrag>): Set<AndelTilkjentYtelseForUtbetalingsoppdrag> {
    return this.filter { a ->
        other.none { b -> a.erTilsvarendeForUtbetaling(b) }
    }.toSet()
}

private fun AndelTilkjentYtelseForUtbetalingsoppdrag.erTilsvarendeForUtbetaling(other: AndelTilkjentYtelseForUtbetalingsoppdrag): Boolean {
    return (
        this.aktør == other.aktør &&
            this.stønadFom == other.stønadFom &&
            this.stønadTom == other.stønadTom &&
            this.kalkulertUtbetalingsbeløp == other.kalkulertUtbetalingsbeløp &&
            this.type == other.type
        )
}
