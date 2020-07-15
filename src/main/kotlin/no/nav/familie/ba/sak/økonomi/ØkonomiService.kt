package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.YtelseType
import no.nav.familie.ba.sak.common.RessursUtils.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ØkonomiService(
        private val økonomiKlient: ØkonomiKlient,
        private val behandlingService: BehandlingService,
        private val behandlingResultatService: BehandlingResultatService,
        private val vedtakService: VedtakService,
        private val beregningService: BeregningService,
        private val utbetalingsoppdragGenerator: UtbetalingsoppdragGenerator
) {

    fun separerNyeOgOpphørteAndelerForØkonomi(behandlingId: Long,
                                              forrigeBehandlingId: Long): Pair<List<AndelTilkjentYtelse>, List<AndelTilkjentYtelse>> {
        val forrigeTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandlingId).toSet()
        val oppdatertTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(behandlingId).toSet()
        val andelerSomErNye = oppdatertTilstand.subtractAndeler(forrigeTilstand).toList()
        val andelerSomOpphøres = forrigeTilstand.subtractAndeler(oppdatertTilstand).toList()
        return Pair(andelerSomErNye, andelerSomOpphøres)
    }

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

    fun oppdaterTilkjentYtelseOgIverksettVedtak(vedtakId: Long, saksbehandlerId: String) {
        // TODO: https://github.com/navikt/familie-ba-sak/blob/4c78bce83387001c952bae8452b75ace1e014b61/src/main/kotlin/no/nav/familie/ba/sak/%C3%B8konomi/%C3%98konomiService.kt#L25 . Dobbeltsjekk at det er greit å fjerne parameter behandlingsId
        val vedtak = vedtakService.hent(vedtakId)

        val nyesteBehandling = vedtak.behandling
        val oppdatertTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(nyesteBehandling.id)
        val nyeKjeder = delOppIKjederMedIdentifikator(oppdatertTilstand)

        val behandlingResultatType =
                if (nyesteBehandling.type == BehandlingType.TEKNISK_OPPHØR
                    || nyesteBehandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT)
                    BehandlingResultatType.OPPHØRT
                else behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = nyesteBehandling.id)

        val erFørsteBehandlingPåFagsak = behandlingService.hentBehandlinger(nyesteBehandling.fagsak.id).size == 1


        // TODO: Trekk ut henting av oppdatert tilstand før alt skjer, siden det uansett må brukes i begge. Sørg for at samme format blir brukt på forrige når det er behov.

        val (nyeandeler: List<List<AndelTilkjentYtelse>>, opphørandeler: List<Pair<AndelTilkjentYtelse, LocalDate>>) = if (erFørsteBehandlingPåFagsak) {
            val nyeandeler: List<List<AndelTilkjentYtelse>> = nyeKjeder.values.toList()
            Pair(nyeandeler, listOf<Pair<AndelTilkjentYtelse, LocalDate>>())
        } else {
            val forrigeBehandling = vedtakService.hent(vedtak.forrigeVedtakId!!).behandling
            val forrigeTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandling.id)
            val forrigeKjeder = delOppIKjederMedIdentifikator(forrigeTilstand)

            val førsteDirtyIHverKjede = finnFørsteDirtyIHverKjede(forrigeKjeder, nyeKjeder)

            val andelerSomSkalLagesNye: List<List<AndelTilkjentYtelse>> = nyeKjeder.map { (kjedeIdentifikator, kjedeAndeler) ->
                kjedeAndeler.filter { it.stønadFom.isSameOrAfter(førsteDirtyIHverKjede[kjedeIdentifikator]!!) }
            }
            val andelerSomSkaLagesOpphørAvMeDato = forrigeKjeder.map { (kjedeIdentifikator, kjedeAndeler) ->
                val sisteAndelIKjede = kjedeAndeler.sortedBy { it.stønadFom }.last()
                Pair(sisteAndelIKjede, førsteDirtyIHverKjede[kjedeIdentifikator]!!)
            }

            if (behandlingResultatType == BehandlingResultatType.OPPHØRT
                && (andelerSomSkalLagesNye.isNotEmpty() || andelerSomSkaLagesOpphørAvMeDato.isEmpty())) {
                throw IllegalStateException("Kan ikke oppdatere tilkjent ytelse og iverksette vedtak fordi opphør inneholder nye " +
                                            "andeler eller mangler opphørte andeler.")
            }
            Pair(andelerSomSkalLagesNye,
                 andelerSomSkaLagesOpphørAvMeDato)
        }

        val utbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId,
                vedtak,
                behandlingResultatType,
                erFørsteBehandlingPåFagsak,
                kjedefordelteNyeAndeler = nyeandeler,
                kjedefordelteOpphørMedDato = opphørandeler
        )

        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(nyesteBehandling, utbetalingsoppdrag)
        iverksettOppdrag(nyesteBehandling.id, utbetalingsoppdrag)
    }


    private fun iverksettOppdrag(behandlingsId: Long,
                                 utbetalingsoppdrag: Utbetalingsoppdrag) {
        Result.runCatching { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
                .fold(
                        onSuccess = {
                            assertGenerelleSuksessKriterier(it.body)

                            behandlingService.oppdaterStatusPåBehandling(behandlingsId, BehandlingStatus.SENDT_TIL_IVERKSETTING)
                        },
                        onFailure = {
                            throw Exception("Iverksetting mot oppdrag feilet", it)
                        }
                )
    }

    fun hentStatus(oppdragId: OppdragId): OppdragStatus {
        Result.runCatching { økonomiKlient.hentStatus(oppdragId) }
                .fold(
                        onSuccess = {
                            assertGenerelleSuksessKriterier(it.body)
                            return it.body?.data!!
                        },
                        onFailure = {
                            throw Exception("Henting av status mot oppdrag feilet", it)
                        }
                )
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

    companion object {

        val SMÅBARNSTILLEGG_SUFFIX = "_SMÅBARNSTILLEGG"

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

    }
}

