package no.nav.familie.ba.sak.ekstern.pensjon

import no.nav.familie.ba.sak.common.EksternTjenesteFeil
import no.nav.familie.ba.sak.common.EksternTjenesteFeilException
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggle
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
import no.nav.familie.ba.sak.kjerne.tidslinje.tilTidslinje
import no.nav.familie.ba.sak.task.HentAlleIdenterTilPsysTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random

@Service
class PensjonService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val fagsakRepository: FagsakRepository,
    private val personidentService: PersonidentService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val taskRepository: TaskRepositoryWrapper,
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val envService: EnvService,
    private val unleashNext: UnleashNextMedContextService,
) {
    fun hentBarnetrygd(
        personIdent: String,
        fraDato: LocalDate,
    ): List<BarnetrygdTilPensjon> {
        secureLogger.info("Henter data til pensjon for personIdent=$personIdent fraDato=$fraDato")
        if (envService.erPreprod() && unleashNext.isEnabled(FeatureToggle.HENT_IDENTER_TIL_PSYS_FRA_INFOTRYGD)) {
            val barnetrygdTilPensjonFraInfotrygdQ = hentBarnetrygdTilPensjonFraInfotrygdQ(personIdent, fraDato)
            if (barnetrygdTilPensjonFraInfotrygdQ.isNotEmpty()) {
                return barnetrygdTilPensjonFraInfotrygdQ.distinct()
            }
        }
        val aktør = personidentService.hentAktør(personIdent)
        val fagsak = fagsakRepository.finnFagsakForAktør(aktør)
        val barnetrygdTilPensjon = fagsak?.let { hentBarnetrygdTilPensjon(fagsak, fraDato) }
        val (barnetrygdTilPensjonFraInfotrygd, barnetrygdFraRelaterteInfotrygdsaker) =
            hentBarnetrygdTilPensjonFraInfotrygd(aktør, fraDato, barnetrygdTilPensjon?.barna)

        if (barnetrygdTilPensjon == null && barnetrygdTilPensjonFraInfotrygd.barnetrygdPerioder.isEmpty()) return emptyList()

        // Sjekk om det finnes relaterte saker, dvs om barna finnes i andre behandlinger
        val barnetrygdMedRelaterteSaker =
            barnetrygdTilPensjon
                ?.barnetrygdPerioder
                ?.filter { it.personIdent != aktør.aktivFødselsnummer() }
                ?.map { it.personIdent }
                ?.distinct()
                ?.map { hentBarnetrygdForRelatertPersonTilPensjon(it, fraDato, aktør) }
                ?.flatten()
                ?: emptyList()

        return barnetrygdMedRelaterteSaker
            .plus(barnetrygdFraRelaterteInfotrygdsaker)
            .plus(
                barnetrygdTilPensjonFraInfotrygd.plus(barnetrygdTilPensjon),
            ).minusEventuelleInfotrygdperioderSomOverlapperBA(personIdent, fraDato)
    }

    fun lagTaskForHentingAvIdenterTilPensjon(år: Int): String {
        val uuid = UUID.randomUUID()
        taskRepository.save(HentAlleIdenterTilPsysTask.lagTask(år, uuid))
        return uuid.toString()
    }

    private fun hentBarnetrygdForRelatertPersonTilPensjon(
        personIdent: String,
        fraDato: LocalDate,
        forelderAktør: Aktør,
    ): List<BarnetrygdTilPensjon> {
        val aktør = personidentService.hentAktør(personIdent)
        val fagsaker =
            fagsakRepository
                .finnFagsakerSomHarAndelerForAktør(aktør)
                .filter { it.type == FagsakType.NORMAL } // skal kun ha normale fagsaker til med her
                .filter { it.aktør != forelderAktør } // trenger ikke å hente data til forelderen på nytt
                .distinct()
        return fagsaker.mapNotNull { fagsak -> hentBarnetrygdTilPensjon(fagsak, fraDato) }
    }

    private fun hentBarnetrygdTilPensjonFraInfotrygd(
        aktør: Aktør,
        fraDato: LocalDate,
        barnFnr: List<String>?,
    ): Pair<BarnetrygdTilPensjon, List<BarnetrygdTilPensjon>> {
        val personidenter = if (envService.erPreprod()) testidenter(fraDato) else aktør.personidenter.map { it.fødselsnummer }

        val allePerioderTilhørendeAktør = mutableListOf<BarnetrygdPeriode>()
        val barnetrygdFraRelaterteSaker = mutableListOf<BarnetrygdTilPensjon>()

        personidenter.forEach { ident ->
            infotrygdBarnetrygdClient.hentBarnetrygdTilPensjon(ident, fraDato).fagsaker.forEach {
                if (personidenter.contains(it.fagsakEiersIdent)) {
                    allePerioderTilhørendeAktør.addAll(it.barnetrygdPerioder.maskerPersonidenteneIPreprod(barnFnr))
                } else if (!envService.erPreprod()) { // tar ikke med relaterte saker i test fra Q2 i denne omgang. Må i så fall maskeres
                    barnetrygdFraRelaterteSaker.add(it)
                }
            }
        }

        return BarnetrygdTilPensjon(
            fagsakEiersIdent = aktør.aktivFødselsnummer(),
            barnetrygdPerioder = allePerioderTilhørendeAktør,
        ) to barnetrygdFraRelaterteSaker
    }

    private fun hentBarnetrygdTilPensjonFraInfotrygdQ(
        personIdent: String,
        fraDato: LocalDate,
    ): List<BarnetrygdTilPensjon> = infotrygdBarnetrygdClient.hentBarnetrygdTilPensjon(personIdent, fraDato).fagsaker

    private fun hentBarnetrygdTilPensjon(
        fagsak: Fagsak,
        fraDato: LocalDate,
    ): BarnetrygdTilPensjon? {
        val behandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsak.id)
                ?: return null
        logger.info("Henter perioder med barnetrygd til pensjon for fagsakId=${fagsak.id}, behandlingId=${behandling.id}")

        val perioder = hentPerioder(behandling, fraDato)

        return BarnetrygdTilPensjon(
            barnetrygdPerioder = perioder,
            fagsakEiersIdent = fagsak.aktør.aktivFødselsnummer(),
        )
    }

    private fun hentPerioder(
        behandling: Behandling,
        fraDato: LocalDate,
    ): List<BarnetrygdPeriode> {
        val tilkjentYtelse =
            tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id)
                ?: error("Finner ikke tilkjent ytelse for behandling=${behandling.id}")
        return tilkjentYtelse.andelerTilkjentYtelse
            .filter { it.stønadTom.isSameOrAfter(fraDato.toYearMonth()) }
            .filter { it.type == YtelseType.ORDINÆR_BARNETRYGD } // Pensjon trenger kun forholde seg til periodene med Ordinær BA
            .map { andel ->
                BarnetrygdPeriode(
                    ytelseTypeEkstern = andel.type.tilPensjonYtelsesType(),
                    personIdent = andel.aktør.aktivFødselsnummer(),
                    utbetaltPerMnd = andel.kalkulertUtbetalingsbeløp,
                    stønadFom = andel.stønadFom,
                    stønadTom = andel.stønadTom,
                    delingsprosentYtelse =
                        when (andel.prosent) {
                            BigDecimal.valueOf(100L) -> YtelseProsent.FULL
                            BigDecimal.valueOf(50L) -> YtelseProsent.DELT
                            else -> YtelseProsent.USIKKER
                        },
                    norgeErSekundærlandMedNullUtbetaling = andel.differanseberegnetPeriodebeløp?.let { it < 0 } ?: false,
                    sakstypeEkstern = behandling.kategori.tilPensjonSakstype(),
                )
            }
    }

    private fun testidenter(
        fraDato: LocalDate,
    ) = if (unleashNext.isEnabled(FeatureToggle.HENT_IDENTER_TIL_PSYS_FRA_INFOTRYGD)) {
        emptyList() // Skal egentlig ikke kunne havne her, tror jeg, siden hentBarnetrygd() i dette tilfellet skulle returnert på linje 54 for å unngå at det på linje 57 forsøkes å hente data fra pdl på en ident som ikke finnes der i testmiljø
    } else {
        listOfNotNull(
            tilfeldigUttrekkInfotrygdBaQ(fraDato.year),
        )
    }

    fun tilfeldigUttrekkInfotrygdBaQ(
        år: Int,
    ): String? {
        require(envService.erPreprod())
        return when {
            Random.nextBoolean() -> {
                infotrygdBarnetrygdClient.hentPersonerMedBarnetrygdTilPensjon(år).random()
            }

            else -> null
        }
    }

    private fun List<BarnetrygdPeriode>.maskerPersonidenteneIPreprod(barnFnr: List<String>?) =
        when {
            envService.erPreprod() -> {
                val barnetrygdperioerPerPerson = this.groupBy { it.personIdent }.values
                if (barnFnr != null) {
                    barnetrygdperioerPerPerson
                        .zip(barnFnr) { barnetrydperioder, barn ->
                            barnetrydperioder.map { it.copy(personIdent = barn) }
                        }.flatten()
                } else {
                    emptyList()
                }
            }

            else -> this
        }

    fun YtelseType.tilPensjonYtelsesType(): YtelseTypeEkstern =
        when (this) {
            YtelseType.ORDINÆR_BARNETRYGD -> YtelseTypeEkstern.ORDINÆR_BARNETRYGD
            YtelseType.SMÅBARNSTILLEGG -> YtelseTypeEkstern.SMÅBARNSTILLEGG
            YtelseType.UTVIDET_BARNETRYGD -> YtelseTypeEkstern.UTVIDET_BARNETRYGD
        }

    fun BehandlingKategori.tilPensjonSakstype(): SakstypeEkstern =
        when (this) {
            BehandlingKategori.NASJONAL -> SakstypeEkstern.NASJONAL
            BehandlingKategori.EØS -> SakstypeEkstern.EØS
        }

    private fun List<BarnetrygdTilPensjon>.minusEventuelleInfotrygdperioderSomOverlapperBA(
        personIdent: String,
        fraDato: LocalDate,
    ): List<BarnetrygdTilPensjon> =
        distinct().groupBy { it.fagsakEiersIdent }.map { (fagsakEiersIdent, barnetrygdTilPensjon) ->
            val perioderUtenOverlappMellomFagsystemene =
                barnetrygdTilPensjon
                    .flatMap { it.barnetrygdPerioder }
                    .groupBy { it.personIdent }
                    .values
                    .fold(emptyList<BarnetrygdPeriode>()) { acc, perioderTilhørendePerson ->
                        try {
                            acc + perioderTilhørendePerson.distinct().fjernOverlappendeInfotrygdperioder()
                        } catch (e: Exception) {
                            logger.error("Klarte ikke kombinere BA og IT-perioder for fjerning av eventuelle overlapp")
                            secureLogger.warn("Klarte ikke kombinere ba-perioder og infotrygd-perioder", e)

                            throw EksternTjenesteFeilException(
                                eksternTjenesteFeil = EksternTjenesteFeil("/api/ekstern/pensjon/hent-barnetrygd"),
                                melding = "Det oppstod feil ved kombinering av BA og IT-perioder for fjerning av evt. overlapp",
                                request = BarnetrygdTilPensjonRequest(personIdent, fraDato),
                                throwable = e,
                            )
                        }
                    }

            BarnetrygdTilPensjon(
                fagsakEiersIdent = fagsakEiersIdent,
                barnetrygdPerioder = perioderUtenOverlappMellomFagsystemene,
            )
        }

    private fun List<BarnetrygdPeriode>.fjernOverlappendeInfotrygdperioder(): List<BarnetrygdPeriode> {
        val (baSakPerioder, opprinneligeInfotrygdPerioder) =
            partition { it.kildesystem == "BA" }

        val infotrygdperioderSomIkkeOverlapperBaPerioder =
            baSakPerioder
                .tilTidslinje()
                .kombinerMed(opprinneligeInfotrygdPerioder.tilTidslinje()) { periodeIBa, periodeIInfotrygd ->
                    periodeIInfotrygd.takeIf { periodeIBa == null }
                }.perioder()
                .mapNotNull {
                    it.innhold?.copy(
                        stønadFom = it.fraOgMed.tilYearMonth(),
                        stønadTom = it.tilOgMed.tilYearMonth(),
                    )
                }

        sjekkOgLoggOmDetFinnesOverlapp(baSakPerioder, opprinneligeInfotrygdPerioder, infotrygdperioderSomIkkeOverlapperBaPerioder)
        return baSakPerioder + infotrygdperioderSomIkkeOverlapperBaPerioder
    }

    fun sjekkOgLoggOmDetFinnesOverlapp(
        baSak: List<BarnetrygdPeriode>,
        infotrygdperioder: List<BarnetrygdPeriode>,
        infotrygdperioderMinusOverlappeneMedBA: List<BarnetrygdPeriode>,
    ) {
        if (infotrygdperioder != infotrygdperioderMinusOverlappeneMedBA) {
            secureLogger.warn(
                "Fant overlapp mellom $baSak og $infotrygdperioder. " +
                    "Infotrygdperioder korrigert for overlapp:\n$infotrygdperioderMinusOverlappeneMedBA",
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PensjonService::class.java)
    }
}

private fun List<BarnetrygdPeriode>.tilTidslinje() =
    this
        .map {
            Periode(
                fraOgMed = it.stønadFom.tilTidspunkt(),
                // 999999999-12 er Infotrygds definisjon av uendelighet, klippes til 9999-12 for å kunne brukes i tidslinje. Kan fjernes når vi ikke lenger har løpende saker i infotrygd
                tilOgMed = if (it.stønadTom > YearMonth.of(9999, 12)) YearMonth.of(9999, 12).tilTidspunkt() else it.stønadTom.tilTidspunkt(),
                innhold = it,
            )
        }.tilTidslinje()

private operator fun BarnetrygdTilPensjon.plus(other: BarnetrygdTilPensjon?): List<BarnetrygdTilPensjon> =
    when {
        barnetrygdPerioder.isEmpty() -> {
            listOfNotNull(other)
        }

        fagsakEiersIdent == other?.fagsakEiersIdent -> {
            listOf(copy(barnetrygdPerioder = barnetrygdPerioder + other.barnetrygdPerioder))
        }

        else -> {
            listOfNotNull(this, other)
        }
    }

private val BarnetrygdTilPensjon.barna: List<String>
    get() = barnetrygdPerioder.map { it.personIdent }.filter { it != fagsakEiersIdent }
