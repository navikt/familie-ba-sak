package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse.Companion.disjunkteAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse.Companion.snittAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import java.time.YearMonth

object ØkonomiUtils {

    /**
     * Deler andeler inn i gruppene de skal kjedes i. Utbetalingsperioder kobles i kjeder per person, bortsett fra
     * småbarnstillegg og utvidet barnetrygd som separeres i to kjeder for søker. På grunn av dette legges et suffix
     * på småbarnstillegg når vi arbeider med map.
     *
     * @param[andelerForInndeling] andeler som skal sorteres i grupper for kjeding
     * @return ident med kjedegruppe.
     */
    fun kjedeinndelteAndeler(andelerForInndeling: List<AndelTilkjentYtelse>): Map<String, List<AndelTilkjentYtelse>> {
        val (personMedSmåbarnstilleggAndeler, personerMedAndeler) =
            andelerForInndeling.partition { it.type == YtelseType.SMÅBARNSTILLEGG }.toList().map {
                it.groupBy { andel -> andel.aktør.aktivFødselsnummer() }
            }
        val andelerForKjeding = mutableMapOf<String, List<AndelTilkjentYtelse>>()
        andelerForKjeding.putAll(personerMedAndeler)

        if (personMedSmåbarnstilleggAndeler.size > 1) {
            throw IllegalArgumentException("Finnes flere personer med småbarnstillegg")
        } else if (personMedSmåbarnstilleggAndeler.size == 1) {
            val søkerIdent = personMedSmåbarnstilleggAndeler.keys.first()
            andelerForKjeding[søkerIdent + SMÅBARNSTILLEGG_SUFFIX] = personMedSmåbarnstilleggAndeler[søkerIdent]!!
        }
        return andelerForKjeding
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
        forrigeKjeder: Map<String, List<AndelTilkjentYtelse>>,
        oppdaterteKjeder: Map<String, List<AndelTilkjentYtelse>>
    ): Map<String, AndelTilkjentYtelse?> {
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
        forrigeKjeder: Map<String, List<AndelTilkjentYtelse>>,
        oppdaterteKjeder: Map<String, List<AndelTilkjentYtelse>>
    ): Map<String, AndelTilkjentYtelse?> =
        forrigeKjeder.keys.union(oppdaterteKjeder.keys).associateWith { null }

    private fun beståendeAndelerIKjede(
        forrigeKjede: List<AndelTilkjentYtelse>?,
        oppdatertKjede: List<AndelTilkjentYtelse>?
    ): List<AndelTilkjentYtelse>? {
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
    fun finnBeståendeAndelerMedOppdatertOffset(
        oppdaterteKjeder: Map<String, List<AndelTilkjentYtelse>>,
        forrigeKjeder: Map<String, List<AndelTilkjentYtelse>>
    ): List<OffsetOppdatering> = oppdaterteKjeder
        .filter { forrigeKjeder.containsKey(it.key) }
        .flatMap { (kjedeIdentifikator, oppdatertKjede) ->
            beståendeAndelerIKjede(
                forrigeKjede = forrigeKjeder.getValue(kjedeIdentifikator),
                oppdatertKjede = oppdatertKjede
            )?.map { bestående ->
                OffsetOppdatering(
                    oppdatertKjede.find { it.erTilsvarendeForUtbetaling(bestående) }
                        ?: error("Kan ikke finne andel fra utledet bestående andeler i oppdatert tilstand."),
                    bestående.periodeOffset,
                    bestående.forrigePeriodeOffset,
                    bestående.kildeBehandlingId
                )
            } ?: listOf()
        }

    /**
     * Tar utgangspunkt i ny tilstand og finner andeler som må bygges opp (nye, endrede og bestående etter første endring)
     *
     * @param[oppdaterteKjeder] ny tilstand
     * @param[sisteBeståendeAndelIHverKjede] andeler man må bygge opp etter
     * @return andeler som må bygges fordelt på kjeder
     */
    fun andelerTilOpprettelse(
        oppdaterteKjeder: Map<String, List<AndelTilkjentYtelse>>,
        sisteBeståendeAndelIHverKjede: Map<String, AndelTilkjentYtelse?>
    ): List<List<AndelTilkjentYtelse>> =
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
        forrigeKjeder: Map<String, List<AndelTilkjentYtelse>>,
        sisteBeståendeAndelIHverKjede: Map<String, AndelTilkjentYtelse?>,
        endretMigreringsDato: YearMonth? = null
    ): List<Pair<AndelTilkjentYtelse, YearMonth>> =
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

    fun gjeldendeForrigeOffsetForKjede(andelerFraForrigeBehandling: Map<String, List<AndelTilkjentYtelse>>): Map<String, Int> =
        andelerFraForrigeBehandling.map { (personIdent, forrigeKjede) ->
            personIdent to (
                forrigeKjede.filter { it.kalkulertUtbetalingsbeløp > 0 }
                    .maxByOrNull { andel -> andel.periodeOffset!! }?.periodeOffset?.toInt()
                    ?: throw IllegalStateException("Andel i kjede skal ha offset")
                )
        }.toMap()

    private fun altIKjedeOpphøres(
        kjedeidentifikator: String,
        sisteBeståendeAndelIHverKjede: Map<String, AndelTilkjentYtelse?>
    ): Boolean = sisteBeståendeAndelIHverKjede[kjedeidentifikator] == null

    private fun andelOpphøres(
        kjedeidentifikator: String,
        andel: AndelTilkjentYtelse,
        sisteBeståendeAndelIHverKjede: Map<String, AndelTilkjentYtelse?>
    ): Boolean = andel.stønadFom > sisteBeståendeAndelIHverKjede[kjedeidentifikator]!!.stønadTom

    const val SMÅBARNSTILLEGG_SUFFIX = "_SMÅBARNSTILLEGG"
}
