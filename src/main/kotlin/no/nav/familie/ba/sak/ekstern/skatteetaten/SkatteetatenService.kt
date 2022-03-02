package no.nav.familie.ba.sak.ekstern.skatteetaten

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPeriode
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioder
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderResponse
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerson
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPersonerResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
class SkatteetatenService(
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val fagsakRepository: FagsakRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository
) {

    fun finnPersonerMedUtvidetBarnetrygd(år: String): SkatteetatenPersonerResponse {
        LOG.debug("enter finnPersonerMedUtvidetBarnetrygd(), år {}", år)
        val personerFraInfotrygd = infotrygdBarnetrygdClient.hentPersonerMedUtvidetBarnetrygd(år)
        LOG.debug("hent personer fra Infotrygd return {} personer", personerFraInfotrygd.brukere.size)
        val personerFraBaSak = hentPersonerMedUtvidetBarnetrygd(år)
        LOG.debug("hent personer fra ba-sak return {} personer", personerFraBaSak.size)

        val personIdentSet = personerFraBaSak.map { it.ident }.toSet()

        // Assumes that vedtak in ba-sak is always newer than that in Infotrygd for the same person ident
        val kombinertListe =
            personerFraBaSak + personerFraInfotrygd.brukere.filter { !personIdentSet.contains(it.ident) }
        LOG.debug("kombinert person fra Infotrygd og ba-sak: {} unik personer", kombinertListe.size)

        return SkatteetatenPersonerResponse(kombinertListe)
    }

    fun finnPerioderMedUtvidetBarnetrygd(personer: List<String>, år: String): SkatteetatenPerioderResponse {
        val unikePersoner = personer.distinct()
        val perioderFraBaSak = hentPerioderMedUtvidetBarnetrygdFraBaSak(unikePersoner, år)
        val perioderFraInfotrygd =
            infotrygdBarnetrygdClient.hentPerioderMedUtvidetBarnetrygdForPersoner(unikePersoner, år)

        val samletPerioder = mutableListOf<SkatteetatenPerioder>()
        unikePersoner.forEach {
            val resultatInfotrygdForPerson =
                perioderFraInfotrygd.firstOrNull { perioder -> perioder.ident == it }
            val perioderFraInfotrygd =
                resultatInfotrygdForPerson?.perioder ?: emptyList()
            val resutatBaSakForPerson = perioderFraBaSak.firstOrNull { perioder -> perioder.ident == it }
            val perioderFraBasak = resutatBaSakForPerson?.perioder ?: emptyList()
            val perioder =
                (perioderFraBasak + perioderFraInfotrygd).groupBy { periode -> periode.delingsprosent }.values
                    .flatMap(::slåSammenSkatteetatenPeriode).toMutableList()
            if (perioder.isNotEmpty()) {
                samletPerioder.add(
                    SkatteetatenPerioder(
                        ident = it,
                        perioder = perioder,
                        sisteVedtakPaaIdent = resutatBaSakForPerson?.sisteVedtakPaaIdent
                            ?: resultatInfotrygdForPerson!!.sisteVedtakPaaIdent
                    )
                )
            }
        }

        // Assumes that vedtak in ba-sak is always newer than that in Infotrygd for the same person ident
        return SkatteetatenPerioderResponse(samletPerioder)
    }

    private fun hentPersonerMedUtvidetBarnetrygd(år: String): List<SkatteetatenPerson> {
        return fagsakRepository.finnFagsakerMedUtvidetBarnetrygdInnenfor(
            fom = YearMonth.of(år.toInt(), 1),
            tom = YearMonth.of(år.toInt(), 12)
        )
            .map { SkatteetatenPerson(it.first.aktør.aktivFødselsnummer(), it.second.atStartOfDay()) }
    }

    private fun hentPerioderMedUtvidetBarnetrygdFraBaSak(
        personer: List<String>,
        år: String
    ): List<SkatteetatenPerioder> {
        val stonadPerioder = hentUtvidetStonadPerioderForPersoner(personer, år)

        val skatteetatenPerioderMap =
            stonadPerioder.fold(mutableMapOf<String, SkatteetatenPerioder>()) { perioderMap, period ->
                val ident = period.getIdent()
                val nyList = listOf(
                    SkatteetatenPeriode(
                        fraMaaned = period.getFom().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        delingsprosent = period.getProsent().tilDelingsprosent(),
                        tomMaaned = period.getTom().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                    )
                )
                val samletPerioder = if (perioderMap.containsKey(ident))
                    perioderMap[ident]!!.perioder + nyList
                else nyList
                perioderMap[ident] = SkatteetatenPerioder(ident, period.getEndretDato(), samletPerioder)
                perioderMap
            }

        return skatteetatenPerioderMap.toList().map {
            // Slå sammen perioder basert på delingsprosent
            SkatteetatenPerioder(
                ident = it.second.ident,
                sisteVedtakPaaIdent = it.second.sisteVedtakPaaIdent,
                perioder = it.second.perioder.groupBy { periode -> periode.delingsprosent }.values
                    .flatMap(::slåSammenSkatteetatenPeriode).toMutableList()
            )
        }
    }

    private fun hentUtvidetStonadPerioderForPersoner(
        personIdenter: List<String>,
        år: String
    ): List<AndelTilkjentYtelsePeriode> {
        val yearStart = LocalDateTime.of(år.toInt(), 1, 1, 0, 0, 0)
        val yearEnd = LocalDateTime.of(år.toInt(), 12, 31, 23, 59, 59)
        return andelTilkjentYtelseRepository.finnStonadPeriodMedUtvidetBarnetrygdForPersoner(
            personIdenter,
            yearStart,
            yearEnd
        )
    }

    private fun slåSammenSkatteetatenPeriode(perioderAvEtGittDelingsprosent: List<SkatteetatenPeriode>): List<SkatteetatenPeriode> {
        return perioderAvEtGittDelingsprosent.sortedBy { it.fraMaaned }
            .fold(mutableListOf()) { sammenslåttePerioder, nesteUtbetaling ->
                val nesteUtbetalingFraaMåned = YearMonth.parse(nesteUtbetaling.fraMaaned)
                if (sammenslåttePerioder.lastOrNull()?.tomMaaned == nesteUtbetalingFraaMåned.minusMonths(1)
                    .toString()
                ) {
                    sammenslåttePerioder.apply { add(removeLast().copy(tomMaaned = nesteUtbetaling.tomMaaned)) }
                } else sammenslåttePerioder.apply { add(nesteUtbetaling) }
            }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(SkatteetatenService::class.java)
    }
}

fun String.tilDelingsprosent(): SkatteetatenPeriode.Delingsprosent =
    if (this == "100") SkatteetatenPeriode.Delingsprosent._0 else if (this == "50")
        SkatteetatenPeriode.Delingsprosent._50 else SkatteetatenPeriode.Delingsprosent.usikker

fun SkatteetatenPeriode.Delingsprosent.tilBigDecimal(): BigDecimal = when (this) {
    SkatteetatenPeriode.Delingsprosent._0 -> BigDecimal.valueOf(100)
    SkatteetatenPeriode.Delingsprosent._50 -> BigDecimal.valueOf(50)
    else -> BigDecimal.valueOf(0)
}
