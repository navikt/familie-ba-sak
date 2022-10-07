package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse.Companion.disjunkteAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse.Companion.snittAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import java.time.YearMonth

object ØkonomiUtils {

    fun kjedeinndelteAndeler(andelerForInndeling: List<AndelTilkjentYtelse>): Map<KjedeId, List<AndelTilkjentYtelse>> =
        andelerForInndeling
            .groupBy { andel -> andel.tilKjedeId() }
            // Rart at dette må sjekkes her. Det burde vært håndtert i vilkårsvurderingen eller når andelene bygges opp
            // Utvidet barnetrygd valideres altså IKKE, selv om det burde oppføre seg likt(?)
            .also { validerKunEnYtelse(it, YtelseType.SMÅBARNSTILLEGG) }
            // Mange tester forventer at kjedene kommer sortert med småbarnstilleg til sist. Burde være unødvendig
            // Denne sprteringen forutsetter at YtelseType-enum'en er i rekkefølgen: ordinær, utvidet, småbarnstillegg
            .toSortedMap(compareBy({ it.type.ordinal }, { it.aktør.aktivFødselsnummer() }))

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
        forrigeKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>,
        oppdaterteKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>
    ): Map<KjedeId, AndelTilkjentYtelse?> {
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
        forrigeKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>,
        oppdaterteKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>
    ): Map<KjedeId, AndelTilkjentYtelse?> =
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
    fun oppdaterBeståendeAndelerMedOffset(
        oppdaterteKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>,
        forrigeKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>
    ): Map<KjedeId, List<AndelTilkjentYtelse>> {
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
     * Finner eksisterende offset og kilde på andeler som skal bestå
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[oppdaterteKjeder] nåværende tilstand
     * @return liste over oppdateringer som skal utføres
     */
    fun finnBeståendeAndelerMedOffsetSomMåOppdateres(
        oppdaterteKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>,
        forrigeKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>
    ): List<OffsetOppdatering> = oppdaterteKjeder
        .filter { forrigeKjeder.containsKey(it.key) }
        .flatMap { (kjedeIdentifikator, oppdatertKjede) ->
            beståendeAndelerIKjede(
                forrigeKjede = forrigeKjeder.getValue(kjedeIdentifikator),
                oppdatertKjede = oppdatertKjede
            )?.mapNotNull { bestående ->
                val offsetOppdatering = OffsetOppdatering(
                    beståendeAndelSomSkalHaOppdatertOffset = oppdatertKjede.find {
                        it.erTilsvarendeForUtbetaling(
                            bestående
                        )
                    }
                        ?: error("Kan ikke finne andel fra utledet bestående andeler i oppdatert tilstand."),
                    periodeOffset = bestående.periodeOffset,
                    forrigePeriodeOffset = bestående.forrigePeriodeOffset,
                    kildeBehandlingId = bestående.kildeBehandlingId
                )
                if (offsetOppdatering.erGyldigOppdatering()) offsetOppdatering else null
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
        oppdaterteKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>,
        sisteBeståendeAndelIHverKjede: Map<KjedeId, AndelTilkjentYtelse?>
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
        forrigeKjeder: Map<KjedeId, List<AndelTilkjentYtelse>>,
        sisteBeståendeAndelIHverKjede: Map<KjedeId, AndelTilkjentYtelse?>,
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

    fun gjeldendeForrigeOffsetForKjede(andelerFraForrigeBehandling: Map<KjedeId, List<AndelTilkjentYtelse>>): Map<KjedeId, Int> =
        andelerFraForrigeBehandling.mapValues { (_, forrigeKjede) ->
            forrigeKjede.filter { it.kalkulertUtbetalingsbeløp > 0 }
                .maxOf { andel -> andel.periodeOffset!! }.toInt()
        }

    private fun altIKjedeOpphøres(
        kjedeidentifikator: KjedeId,
        sisteBeståendeAndelIHverKjede: Map<KjedeId, AndelTilkjentYtelse?>
    ): Boolean = sisteBeståendeAndelIHverKjede[kjedeidentifikator] == null

    private fun andelOpphøres(
        kjedeidentifikator: KjedeId,
        andel: AndelTilkjentYtelse,
        sisteBeståendeAndelIHverKjede: Map<KjedeId, AndelTilkjentYtelse?>
    ): Boolean = andel.stønadFom > sisteBeståendeAndelIHverKjede[kjedeidentifikator]!!.stønadTom
}

fun AndelTilkjentYtelse.tilKjedeId() = KjedeId(this.aktør, this.type)
data class KjedeId(val aktør: Aktør, val type: YtelseType)

fun validerKunEnYtelse(kjeder: Map<KjedeId, List<AndelTilkjentYtelse>>, type: YtelseType) {
    if (kjeder.keys.filter { it.type == type }.count() > 1) {
        throw IllegalArgumentException("Finnes flere personer med ${type.name.lowercase()}")
    }
}
