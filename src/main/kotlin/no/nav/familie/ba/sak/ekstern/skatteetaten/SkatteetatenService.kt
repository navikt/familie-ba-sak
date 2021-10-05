package no.nav.familie.ba.sak.ekstern.skatteetaten

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPeriode
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioder
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderResponse
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerson
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPersonerResponse
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
class SkatteetatenService(
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val fagsakRepository: FagsakRepository
) {

    fun finnPersonerMedUtvidetBarnetrygd(år: String): SkatteetatenPersonerResponse {
        val personerFraInfotrygd = infotrygdBarnetrygdClient.hentPersonerMedUtvidetBarnetrygd(år)
        val personerFraBaSak = hentPersonerMedUtvidetBarnetrygd(år)
        val personIdentSet = personerFraBaSak.map { it.ident }.toSet()

        val kombinertListe = personerFraBaSak + personerFraInfotrygd.brukere.filter { !personIdentSet.contains(it.ident) }

        return SkatteetatenPersonerResponse(kombinertListe)
    }

    fun finnPerioderMedUtvidetBarnetrygd(personer: List<String>, år: String): SkatteetatenPerioderResponse {
        val unikPersoner = personer.toSet().toList()
        val perioderFraBaSak = hentPerioderMedUtvidetBarnetrygdFraBaSak(personer, år)
        val perioderFraInfotrygd = unikPersoner.mapNotNull { infotrygdBarnetrygdClient.hentPerioderMedUtvidetBarnetrygd(it, år) }
        val baSakPersonIdenter = perioderFraBaSak.map { it.ident }.toSet()
        return SkatteetatenPerioderResponse(perioderFraBaSak + perioderFraInfotrygd.filter {
            !baSakPersonIdenter.contains(it.ident)
        })
    }

    private fun hentPersonerMedUtvidetBarnetrygd(år: String): List<SkatteetatenPerson> {
        return fagsakRepository.finnFagsakerMedUtvidetBarnetrygdInnenfor(
            fom = YearMonth.of(år.toInt(), 1),
            tom = YearMonth.of(år.toInt(), 12)
        )
            .map { SkatteetatenPerson(it.first.hentAktivIdent().ident, it.second.atStartOfDay()) }
    }

    private fun hentPerioderMedUtvidetBarnetrygdFraBaSak(personer: List<String>, år: String): List<SkatteetatenPerioder> {
        val stonadPerioder = hentUtdannetStonadPerioderForPersoner(personer, år)
        val SkatteetatenPerioderMap = stonadPerioder.fold(mutableMapOf<String, SkatteetatenPerioder>()) { perioderMap, period ->
            val nyList = listOf(
                SkatteetatenPeriode(
                    fraMaaned = period.getFom().format(DateTimeFormatter.ofPattern("YYYY-MM")),
                    delingsprosent = period.getProsent().tilDelingsprosent(),
                    tomMaaned = period.getTom().format(DateTimeFormatter.ofPattern("YYYY-MM"))
                )
            )
            val samletPerioder = if (perioderMap.containsKey(period.getIdent()))
                perioderMap.get(period.getIdent())!!.perioder + nyList
            else nyList
            perioderMap.put(period.getIdent(), SkatteetatenPerioder(period.getIdent(), period.getOpprettetDato(), samletPerioder))
            perioderMap
        }
        return SkatteetatenPerioderMap.toList().map {
            it.second
        }
    }

    private fun hentUtdannetStonadPerioderForPersoner(personIdenter: List<String>, år: String)
            : List<AndelTilkjentYtelsePeriode> {
        return fagsakRepository.finnStonadPeriodMedUtvidetBarnetrygdForPersoner(
            personIdenter,
            LocalDateTime.of(år.toInt(), 1, 1, 0, 0, 0),
            LocalDateTime.of(år.toInt(), 12, 31, 23, 59, 59),
        )
    }
}

fun String.tilDelingsprosent(): SkatteetatenPeriode.Delingsprosent =
    if (this == "100") SkatteetatenPeriode.Delingsprosent._0 else if (this == "50")
        SkatteetatenPeriode.Delingsprosent._50 else SkatteetatenPeriode.Delingsprosent.usikker

fun SkatteetatenPeriode.Delingsprosent.tilBigDecimal(): BigDecimal = when (this) {
    SkatteetatenPeriode.Delingsprosent._0 -> BigDecimal.valueOf(100);
    SkatteetatenPeriode.Delingsprosent._50 -> BigDecimal.valueOf(50);
    else -> BigDecimal.valueOf(0)
}