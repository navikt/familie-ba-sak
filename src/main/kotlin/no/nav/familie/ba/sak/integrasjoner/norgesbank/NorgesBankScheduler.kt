package no.nav.familie.ba.sak.integrasjoner.norgesbank

import no.nav.familie.leader.LeaderClient
import no.nav.familie.valutakurs.NorgesBankValutakursRestKlient
import no.nav.familie.valutakurs.domene.norgesbank.Frekvens
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Component
class NorgesBankScheduler(
    private val norgesBankValutakursRestKlient: NorgesBankValutakursRestKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(NorgesBankScheduler::class.java)

    @Scheduled(cron = "0 0 0 * * MON-FRI")
    fun dagligSjekkOmValutakursklientFungerer() {
        if (LeaderClient.isLeader() == true) {
            val førsteMandagIForrigeMåned =
                LocalDate
                    .now()
                    .minusMonths(1)
                    .withDayOfMonth(1)
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))

            try {
                norgesBankValutakursRestKlient.hentValutakurs(
                    frekvens = Frekvens.VIRKEDAG,
                    valuta = "SEK",
                    kursDato = førsteMandagIForrigeMåned,
                )
            } catch (e: Exception) {
                logger.error(
                    """Den daglige selftesten mot Norges Bank feilet med verdier SEK og $førsteMandagIForrigeMåned. 
                    |- Sjekk om Norges Bank er nede. 
                    |- Sjekk om API har endret seg. 
                    |
                    |Se README i https://github.com/navikt/familie-felles/tree/main/valutakurs-klient for å kjøre integrasjonstest mot Norges Bank og for linker til dokumentasjonen til Norges Bank
                    |
                    """.trimMargin(),
                    e,
                )
            }
        }
    }
}
