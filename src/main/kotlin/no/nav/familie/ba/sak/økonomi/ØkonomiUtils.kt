package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.isSameOrBefore
import java.time.LocalDate

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
                    it.groupBy { andel -> andel.personIdent } // TODO: Hva skjer dersom personidenten endrer seg? Bør gruppere på en annen måte og oppdatere lagingen av utbetalingsperioder fra andeler
                }
        val andelerForKjeding = mutableMapOf<String, List<AndelTilkjentYtelse>>()
        andelerForKjeding.putAll(personerMedAndeler)

        if (personMedSmåbarnstilleggAndeler.size > 1) {
            throw IllegalArgumentException("Finnes flere personer med småbarnstillegg")
        } else if (personMedSmåbarnstilleggAndeler.size == 1) {
            val søkerIdent = personMedSmåbarnstilleggAndeler.keys.first()
            andelerForKjeding.put(søkerIdent + SMÅBARNSTILLEGG_SUFFIX, personMedSmåbarnstilleggAndeler[søkerIdent]!!)
        }
        return andelerForKjeding
    }

    /**
     * Lager oversikt over datoer vi må opphøre og eventuelt gjenoppbygge hver kjede fra.
     * Personident er identifikator for hver kjede, men unntak av småbarnstillegg som vil være en egen "person".
     *
     * @param[forrigeKjeder] forrige behandlings tilstand
     * @param[nyeKjeder] nåværende tilstand
     * @return map med personident og f.o.m.-dato for første endrede andel for person
     */
    fun dirtyKjedeFomOversikt(forrigeKjeder: Map<String, List<AndelTilkjentYtelse>>,
                              nyeKjeder: Map<String, List<AndelTilkjentYtelse>>): Map<String, LocalDate> {
        val allePersoner = forrigeKjeder.keys.union(nyeKjeder.keys)

        val dirtyKjedeFomOversikt = allePersoner.associate { person ->
            val kjedeDirtyFom = finnFørsteDirtyIKjede(
                    forrigeKjede = forrigeKjeder[person],
                    oppdatertKjede = nyeKjeder[person])
                    ?.stønadFom
            person to kjedeDirtyFom
        }
        return dirtyKjedeFomOversikt.filter { it.value != null } as Map<String, LocalDate>
    }

    /**
     * Finner første andel som ikke finnes i snittet, dvs første andel som ikke er ny, opphøres eller endres.
     * Vi bruker f.o.m.-dato fra denne andelen til å opphøre alt etterfølgende og bygge opp på nytt.
     *
     * @param[forrigeKjede] perioder fra forrige vedtak.
     * @param[oppdatertKjede] perioder fra nytt vedtak
     * @return Første andel som ikke finnes i snitt
     */
    private fun finnFørsteDirtyIKjede(forrigeKjede: List<AndelTilkjentYtelse>?,
                                      oppdatertKjede: List<AndelTilkjentYtelse>?): AndelTilkjentYtelse? {
        val forrige = forrigeKjede?.toSet() ?: emptySet()
        val oppdatert = oppdatertKjede?.toSet() ?: emptySet()
        val alleAndelerMedEndring = forrige.disjunkteAndeler(oppdatert)
        return alleAndelerMedEndring.sortedBy { it.stønadFom }.firstOrNull()
    }

    fun oppdaterteAndelerFraFørsteEndring(oppdaterteKjeder: Map<String, List<AndelTilkjentYtelse>>,
                                          dirtyKjedeFomOversikt: Map<String, LocalDate>): List<List<AndelTilkjentYtelse>> =
            oppdaterteKjeder
                    .map { (kjedeIdentifikator, kjedeAndeler) ->
                        kjedeAndeler.filter { it.stønadFom.isSameOrAfter(dirtyKjedeFomOversikt[kjedeIdentifikator]!!) }
                    }

    fun opphørteAndelerEtterDato(forrigeKjeder: Map<String, List<AndelTilkjentYtelse>>,
                                 dirtyKjedeFomOversikt: Map<String, LocalDate>): List<Pair<AndelTilkjentYtelse, LocalDate>> =
            forrigeKjeder.map { (kjedeIdentifikator, kjedeAndeler) ->
                val sisteAndelIKjede = kjedeAndeler.sortedBy { it.stønadFom }.last()
                Pair(sisteAndelIKjede, dirtyKjedeFomOversikt[kjedeIdentifikator]!!)
            }

    fun sisteOffsetForHverKjede(forrigeKjeder: Map<String, List<AndelTilkjentYtelse>>,
                                dirtyKjedeFomOversikt: Map<String, LocalDate>): Map<String, Int> {
        val personerMedBeståendeOgDirty =
                forrigeKjeder
                        .filter { it.key in dirtyKjedeFomOversikt.keys }
                        .mapValues { (personSomHarEndring, personsForrigeAndeler) ->
                            personsForrigeAndeler
                                    .sortedBy { it.stønadFom }
                                    .filter { it.stønadFom.isSameOrBefore(dirtyKjedeFomOversikt[personSomHarEndring]!!) }
                        }.filter { (personSomHarEndring, andelerFørOppbygging) -> andelerFørOppbygging.isNotEmpty() }
                        .toMap()

        return personerMedBeståendeOgDirty.mapValues { it.value.maxBy { it.stønadFom }!!.periodeOffset!!.toInt() }
    }

    private fun Set<AndelTilkjentYtelse>.subtractAndeler(other: Set<AndelTilkjentYtelse>): Set<AndelTilkjentYtelse> {
        val andelerKunIDenne = mutableSetOf<AndelTilkjentYtelse>()
        this.forEach letEtterTilsvarende@{ a ->
            other.forEach { b ->
                if (a.erTilsvarendeForUtbetaling(b)) {
                    return@letEtterTilsvarende
                }
            }
            andelerKunIDenne.add(a)
        }
        return andelerKunIDenne
    }

    private fun Set<AndelTilkjentYtelse>.disjunkteAndeler(other: Set<AndelTilkjentYtelse>): Set<AndelTilkjentYtelse> {
        val andelerKunIDenne = this.subtractAndeler(other)
        val andelerKunIAnnen = other.subtractAndeler(this)
        return andelerKunIDenne.union(andelerKunIAnnen)
    }

    val SMÅBARNSTILLEGG_SUFFIX = "_SMÅBARNSTILLEGG"
}

