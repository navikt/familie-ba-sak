package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.isSameOrAfter
import java.time.LocalDate

object ØkonomiUtils {

    // TODO: List<AndelTilkjentYtelse> delegate?
    /**
     * Finner første andel som ikke finnes i snittet, dvs første andel som ikke er ny, opphøres eller endres.
     * Vi bruker f.o.m.-dato fra denne andelen til å opphøre alt etterfølgende og bygge opp på nytt.
     *
     * @param[forrigeKjede] perioder fra forrige vedtak. Tom hvis kjeden er ny og disjunkt vil returnere alle fra oppdatert.
     * @param[oppdatertKjede] perioder fra nytt vedtak. Tom hvis kjeden opphøres helt og disjunkt vil returnere alle fra forrige.
     * @return Første andel som ikke finnes i snitt
     */
    fun førsteDirtyAndelIKjede(forrigeKjede: List<AndelTilkjentYtelse>? = emptyList(),
                               oppdatertKjede: List<AndelTilkjentYtelse>? = emptyList()): AndelTilkjentYtelse {
        val forrige = forrigeKjede!!.toSet()
        val oppdatert = oppdatertKjede!!.toSet()
        val alleAndelerMedEndring = forrige.disjunkteAndeler(oppdatert)
        return alleAndelerMedEndring.sortedBy { it.stønadFom }.first()
    }

    /**
     *
     *
     * @return map med person til dato
     */
    fun finnFørsteDirtyIHverKjede(forrigeKjeder: Map<String, List<AndelTilkjentYtelse>>,
                                  nyeKjeder: Map<String, List<AndelTilkjentYtelse>>): Map<String, LocalDate> {

        val identifikatorerForAlleKjederPåFagsak = forrigeKjeder.keys.union(nyeKjeder.keys)

        return identifikatorerForAlleKjederPåFagsak.associate { kjedeIdentifikator ->
            val kjedeDirtyFom = førsteDirtyAndelIKjede(
                    forrigeKjede = forrigeKjeder[kjedeIdentifikator],
                    oppdatertKjede = nyeKjeder[kjedeIdentifikator])
                    .stønadFom
            kjedeIdentifikator to kjedeDirtyFom
        }
    }

    // TODO: List<AndelTilkjentYtelse> delegate?
    // TODO: Kalle forrige og nye for noen skal sitausjonsbilde
    // TODO: Kall heller kjede/kjedegruppe for person og spesifiser at småbarnstillegg er en egen person
    fun oppdaterteAndelerFraFørsteEndring(oppdaterteKjeder: Map<String, List<AndelTilkjentYtelse>>,
                                          førsteEndringForPerson: Map<String, LocalDate>): List<List<AndelTilkjentYtelse>> =
            oppdaterteKjeder
                    .map { (kjedeIdentifikator, kjedeAndeler) ->
                        kjedeAndeler.filter { it.stønadFom.isSameOrAfter(førsteEndringForPerson[kjedeIdentifikator]!!) }
                    }

    fun opphørteAndelerEtterDato(forrigeKjeder: Map<String, List<AndelTilkjentYtelse>>,
                                 førsteEndringForPerson: Map<String, LocalDate>): List<Pair<AndelTilkjentYtelse, LocalDate>> =
            forrigeKjeder.map { (kjedeIdentifikator, kjedeAndeler) ->
                val sisteAndelIKjede = kjedeAndeler.sortedBy { it.stønadFom }.last()
                Pair(sisteAndelIKjede, førsteEndringForPerson[kjedeIdentifikator]!!)
            }

    // TODO: ta en runde på scoping av funksjoner

    fun Set<AndelTilkjentYtelse>.intersectAndeler(other: Set<AndelTilkjentYtelse>): Set<Pair<AndelTilkjentYtelse, AndelTilkjentYtelse>> {
        val andelerIBegge = mutableSetOf<Pair<AndelTilkjentYtelse, AndelTilkjentYtelse>>()
        this.forEach letEtterTilsvarende@{ a ->
            other.forEach { b ->
                if (a.erTilsvarendeForUtbetaling(b)) {
                    andelerIBegge.add(Pair(a, b))
                    return@letEtterTilsvarende
                }
            }
        }
        return andelerIBegge
    }

    fun Set<AndelTilkjentYtelse>.subtractAndeler(other: Set<AndelTilkjentYtelse>): Set<AndelTilkjentYtelse> {
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

    fun Set<AndelTilkjentYtelse>.disjunkteAndeler(other: Set<AndelTilkjentYtelse>): Set<AndelTilkjentYtelse> {
        val andelerKunIDenne = this.subtractAndeler(other)
        val andelerKunIAnnen = other.subtractAndeler(this)
        return andelerKunIDenne.union(andelerKunIAnnen)
    }

    fun delOppIKjederMedIdentifikator(andelerSomSkalSplittes: List<AndelTilkjentYtelse>): Map<String, List<AndelTilkjentYtelse>> {
        val (personMedSmåbarnstilleggAndeler, personerMedAndeler) =
                andelerSomSkalSplittes.partition { it.type == YtelseType.SMÅBARNSTILLEGG }.toList().map {
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

    val SMÅBARNSTILLEGG_SUFFIX = "_SMÅBARNSTILLEGG"
}

