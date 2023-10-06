package no.nav.familie.ba.sak.ekstern.pensjon

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.featureToggle.miljø.Profil
import no.nav.familie.ba.sak.config.featureToggle.miljø.erAktiv
import no.nav.familie.ba.sak.ekstern.bisys.BisysService
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.task.HentAlleIdenterTilPsysTask
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
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
    private val environment: Environment,
) {
    fun hentBarnetrygd(personIdent: String, fraDato: LocalDate): List<BarnetrygdTilPensjon> {
        val aktør = personidentService.hentAktør(personIdent)
        val fagsak = fagsakRepository.finnFagsakForAktør(aktør) ?: return emptyList()
        val barnetrygdTilPensjon = hentBarnetrygdTilPensjon(fagsak, fraDato) ?: return emptyList()
        val barnetrygdTilPensjonFraInfotrygd = hentBarnetrygdTilPensjonFraInfotrygd(aktør, fraDato)

        // Sjekk om det finnes relaterte saker, dvs om barna finnes i andre behandlinger
        val barnetrygdMedRelaterteSaker = barnetrygdTilPensjon.barnetrygdPerioder
            .filter { it.personIdent != aktør.aktivFødselsnummer() }
            .map { it.personIdent }.distinct()
            .map { hentBarnetrygdForRelatertPersonTilPensjon(it, fraDato, aktør) }
            .flatten()
        return barnetrygdMedRelaterteSaker.plus(barnetrygdTilPensjon.plus(barnetrygdTilPensjonFraInfotrygd)).distinct()
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
        val fagsaker = fagsakRepository.finnFagsakerSomHarAndelerForAktør(aktør)
            .filter { it.type == FagsakType.NORMAL } // skal kun ha normale fagsaker til med her
            .filter { it.aktør != forelderAktør } // trenger ikke å hente data til forelderen på nytt
            .distinct()
        return fagsaker.mapNotNull { fagsak -> hentBarnetrygdTilPensjon(fagsak, fraDato) }
    }

    private fun hentBarnetrygdTilPensjonFraInfotrygd(aktør: Aktør, fraDato: LocalDate): BarnetrygdTilPensjon {
        val personidenter = personidenter(aktør)
        val allePerioderTilhørendeAktør = personidenter.flatMap { ident ->
            infotrygdBarnetrygdClient.hentBarnetrygdTilPensjon(ident.fødselsnummer, fraDato).fagsaker.flatMap {
                it.barnetrygdPerioder
            }
        }
        return BarnetrygdTilPensjon(
            fagsakEiersIdent = aktør.aktivFødselsnummer(),
            barnetrygdPerioder = allePerioderTilhørendeAktør,
        )
    }

    private fun hentBarnetrygdTilPensjon(fagsak: Fagsak, fraDato: LocalDate): BarnetrygdTilPensjon? {
        val behandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsak.id)
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
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingAndHasUtbetalingsoppdrag(behandling.id)
            ?: error("Finner ikke tilkjent ytelse for behandling=${behandling.id}")
        return tilkjentYtelse.andelerTilkjentYtelse
            .filter { it.stønadTom.isSameOrAfter(fraDato.toYearMonth()) }
            .filter { it.prosent != BigDecimal.ZERO && it.kalkulertUtbetalingsbeløp != 0 }
            .map {
                BarnetrygdPeriode(
                    ytelseTypeEkstern = it.type.tilPensjonYtelsesType(),
                    personIdent = it.aktør.aktivFødselsnummer(),
                    stønadFom = it.stønadFom,
                    stønadTom = it.stønadTom,
                    delingsprosentYtelse = when (it.prosent) {
                        BigDecimal.valueOf(100L) -> YtelseProsent.FULL
                        BigDecimal.valueOf(50L) -> YtelseProsent.DELT
                        else -> YtelseProsent.USIKKER
                    },
                )
            }
    }

    private fun personidenter(
        aktør: Aktør
    ) = when {
        environment.erAktiv(Profil.Preprod) -> listOfNotNull(
            tilfeldigUttrekkInfotrygdBaQ(aktør.aktivFødselsnummer())?.let { Personident(it, aktør) }
        )

        else -> aktør.personidenter
    }

    @Cacheable("pensjon_testident", cacheManager = "dailyCache")
    fun tilfeldigUttrekkInfotrygdBaQ(forIdent: String): String? {
        require(environment.erAktiv(Profil.Preprod))
        return when {
            Random.nextBoolean() -> {
                infotrygdBarnetrygdClient.hentPersonerMedBarnetrygdTilPensjon(Year.now().value).random()
            }
            else -> null
        }
    }

    fun YtelseType.tilPensjonYtelsesType(): YtelseTypeEkstern {
        return when (this) {
            YtelseType.ORDINÆR_BARNETRYGD -> YtelseTypeEkstern.ORDINÆR_BARNETRYGD
            YtelseType.SMÅBARNSTILLEGG -> YtelseTypeEkstern.SMÅBARNSTILLEGG
            YtelseType.UTVIDET_BARNETRYGD -> YtelseTypeEkstern.UTVIDET_BARNETRYGD
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BisysService::class.java)
    }
}

private operator fun BarnetrygdTilPensjon.plus(other: BarnetrygdTilPensjon): List<BarnetrygdTilPensjon> {
    return if (fagsakEiersIdent == other.fagsakEiersIdent) {
        listOf(copy(barnetrygdPerioder = barnetrygdPerioder + other.barnetrygdPerioder))
    } else {
        listOf(this, other)
    }
}
