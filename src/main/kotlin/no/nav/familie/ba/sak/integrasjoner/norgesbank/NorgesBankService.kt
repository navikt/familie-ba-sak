package no.nav.familie.ba.sak.integrasjoner.norgesbank

import no.nav.familie.ba.sak.common.saner
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBValutakursCache
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBValutakursCacheRepository
import no.nav.familie.valutakurs.NorgesBankValutakursRestKlient
import no.nav.familie.valutakurs.domene.norgesbank.Frekvens
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Import(NorgesBankValutakursRestKlient::class)
class NorgesBankService(
    private val norgesBankValutakursRestKlient: NorgesBankValutakursRestKlient,
    private val ecbValutakursCacheRepository: ECBValutakursCacheRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(NorgesBankService::class.java)

    /**
     * @param utenlandskValuta valutaen vi skal hente kurs for
     * @param kursDato datoen vi skal hente kurs for
     * @return Henter valutakurs for utenlandskValuta -> NOK.
     */
    fun hentValutakurs(
        utenlandskValuta: String,
        kursDato: LocalDate,
    ): BigDecimal {
        val valutakurs = ecbValutakursCacheRepository.findByValutakodeAndValutakursdato(utenlandskValuta, kursDato)?.firstOrNull()
        if (valutakurs == null) {
            logger.info("Henter valutakurs for ${utenlandskValuta.saner()} på $kursDato")
            val valutakurs =
                norgesBankValutakursRestKlient.hentValutakurs(Frekvens.VIRKEDAG, utenlandskValuta, kursDato)
            val lagretValutakurs =
                ecbValutakursCacheRepository.save(
                    ECBValutakursCache(
                        kurs = valutakurs.kurs,
                        valutakode = valutakurs.valuta,
                        valutakursdato = valutakurs.kursDato,
                    ),
                )
            return lagretValutakurs.kurs
        }
        logger.info("Valutakurs ble hentet fra cache for ${utenlandskValuta.saner()} på $kursDato")
        return valutakurs.kurs
    }
}
