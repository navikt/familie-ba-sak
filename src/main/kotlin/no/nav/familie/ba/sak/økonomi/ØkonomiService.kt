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

    /*
    fun hentNyeKjeder(behandlingId: Long, forrigeBehandlingId: Long): Pair<List<AndelTilkjentYtelse>, List<AndelTilkjentYtelse>> {
        val forrigeTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandlingId).toSet()
        val oppdatertTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(behandlingId).toSet()


        val andelerSomErNye = oppdatertTilstand.subtractAndeler(forrigeTilstand).toList()
        val andelerSomOpphøres = forrigeTilstand.subtractAndeler(oppdatertTilstand).toList()

        val kjederForrigeTilstand = delOppIKjeder(forrigeTilstand.toList())
        val kjederOppdatertTilstand = delOppIKjeder(oppdatertTilstand.toList())



        // TODO : Bør gå fint hvis man sørger for at begge er i samme rekkefølge. Sortedwith og then.
        return null
    }

    fun finnFørsteDirtyIKjede(andeler: List<AndelTilkjentYtelse>) {

    }

    fun andelerFomFørsteDirty(behandlingId: Long,
                              forrigeBehandlingId: Long) : List<AndelTilkjentYtelse>{
        val forrigeTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandlingId).toSet()
        val oppdatertTilstand = beregningService.hentAndelerTilkjentYtelseForBehandling(behandlingId).toSet()
        val andelerSomErNye = oppdatertTilstand.subtractAndeler(forrigeTilstand).toList()
        val andelerSomOpphøres = forrigeTilstand.subtractAndeler(oppdatertTilstand).toList()

        val sisteForrige = forrigeTilstand.sortedBy { it.stønadTom }.last()
        val førsteEndrede = andelerSomOpphøres.sortedBy { it.stønadFom }.first()

        val førsteNye = andelerSomErNye.sortedBy { it.stønadFom }.first() // Kan være før opphørFom
        val andelerFomFørsteNye = oppdatertTilstand.filter { it.stønadFom.isSameOrAfter(førsteNye.stønadFom) }
        return null
    }
    */

    fun oppdaterTilkjentYtelseOgIverksettVedtak(vedtakId: Long, saksbehandlerId: String) {
        // TODO: https://github.com/navikt/familie-ba-sak/blob/4c78bce83387001c952bae8452b75ace1e014b61/src/main/kotlin/no/nav/familie/ba/sak/%C3%B8konomi/%C3%98konomiService.kt#L25 . Dobbeltsjekk at det er greit å fjerne parameter behandlingsId
        val vedtak = vedtakService.hent(vedtakId)
        val nyesteBehandling = vedtak.behandling
        val behandlingResultatType =
                if (nyesteBehandling.type == BehandlingType.TEKNISK_OPPHØR
                    || nyesteBehandling.type == BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT)
                    BehandlingResultatType.OPPHØRT
                else behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandlingId = nyesteBehandling.id)

        val erFørsteBehandlingPåFagsak = behandlingService.hentBehandlinger(nyesteBehandling.fagsak.id).size == 1

        val (nyeandeler, opphørandeler) = if (erFørsteBehandlingPåFagsak) {
            val nyeandeler = beregningService.hentAndelerTilkjentYtelseForBehandling(nyesteBehandling.id)
            Pair(nyeandeler, listOf<AndelTilkjentYtelse>())
        } else {
            val forrigeBehandling = vedtakService.hent(vedtak.forrigeVedtakId!!).behandling
            val (andelerSomErNye, andelerSomOpphøres) = separerNyeOgOpphørteAndelerForØkonomi(nyesteBehandling.id,
                                                                                              forrigeBehandling.id)
            if (behandlingResultatType == BehandlingResultatType.OPPHØRT
                && (andelerSomErNye.isNotEmpty() || andelerSomOpphøres.isEmpty())) {
                throw IllegalStateException("Kan ikke oppdatere tilkjent ytelse og iverksette vedtak fordi opphør inneholder nye " +
                                            "andeler eller mangler opphørte andeler.")
            }
            Pair(beregningService.hentAndelerTilkjentYtelseForBehandling(forrigeBehandling.id),
                 beregningService.hentAndelerTilkjentYtelseForBehandling(nyesteBehandling.id))
        }

        // TODO: Nye andeler blir flere enn i dag - inkluderer også de som skjer etterpå men ikke er "nye". Gjenoppbygging
        // TODO: Opphørte andeler vil være par av andel+dato. Husk at smnåbarn må splittes før dette skjer



        // TODO: Skal deles i kjeder før innsending.
        val utbetalingsoppdrag = utbetalingsoppdragGenerator.lagUtbetalingsoppdrag(
                saksbehandlerId,
                vedtak,
                behandlingResultatType,
                erFørsteBehandlingPåFagsak,
                nyeAndeler = nyeandeler,
                opphørteAndeler = opphørandeler
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

    fun delOppIKjeder(andelerSomSkalSplittes: List<AndelTilkjentYtelse>): List<List<AndelTilkjentYtelse>> {
        // Separereer i lister siden småbarnstillegg og utvidet barnetrygd begge vil stå på forelder, men skal kjedes separat
        val (personMedSmåbarnstilleggAndeler, personerMedAndeler) =
                andelerSomSkalSplittes.partition { it.type == YtelseType.SMÅBARNSTILLEGG }.toList().map {
                    it.groupBy { andel -> andel.personIdent } // TODO: Hva skjer dersom personidenten endrer seg? Bør gruppere på en annen måte og oppdatere lagingen av utbetalingsperioder fra andeler
                }
        val andelerForKjeding = listOf(personMedSmåbarnstilleggAndeler.values, personerMedAndeler.values).flatten()
        if (personMedSmåbarnstilleggAndeler.size > 1) {
            throw IllegalArgumentException("Finnes flere personer med småbarnstillegg")
        }
        return andelerForKjeding
    }

    companion object {

        fun Set<AndelTilkjentYtelse>.intersectAndeler(other: Set<AndelTilkjentYtelse>): Set<Pair<AndelTilkjentYtelse, AndelTilkjentYtelse>> {
            val andelerIBegge = mutableSetOf<Pair<AndelTilkjentYtelse, AndelTilkjentYtelse>>()
            this.forEach letEtterTilsvarende@{ a ->
                other.forEach { b ->
                    if (a.erTilsvarendeForUtbetaling(b)) {
                        andelerIBegge.add(Pair(a,b))
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
    }
}

