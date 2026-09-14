package no.nav.familie.ba.sak.integrasjoner.ecb

import no.nav.familie.ba.sak.common.del
import no.nav.familie.ba.sak.common.saner
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBValutakursCache
import no.nav.familie.ba.sak.integrasjoner.ecb.domene.ECBValutakursCacheRepository
import no.nav.familie.valutakurs.ECBValutakursRestKlient
import no.nav.familie.valutakurs.domene.Valutakurs
import no.nav.familie.valutakurs.domene.ecb.Frequency
import no.nav.familie.valutakurs.domene.exchangeRateForCurrency
import no.nav.familie.valutakurs.exception.ValutakursClientException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Import(ECBValutakursRestKlient::class)
class ECBService(
    private val ecbValutakursRestKlient: ECBValutakursRestKlient,
    private val ecbValutakursCacheRepository: ECBValutakursCacheRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(ECBService::class.java)

    /**
     * @param utenlandskValuta valutaen vi skal konvertere til NOK
     * @param kursDato datoen vi skal hente valutakurser for
     * @return Henter valutakurs for *utenlandskValuta* -> EUR og NOK -> EUR på *kursDato*, og returnerer en beregnet kurs for *utenlandskValuta* -> NOK.
     */
    @Throws(ECBServiceException::class)
    fun hentValutakurs(
        utenlandskValuta: String,
        kursDato: LocalDate,
    ): BigDecimal {
        val valutakurs = ecbValutakursCacheRepository.findByValutakodeAndValutakursdato(utenlandskValuta, kursDato)?.firstOrNull()
        if (valutakurs == null) {
            logger.info("Henter valutakurs for ${utenlandskValuta.saner()} på $kursDato")
            try {
                val valutakurser =
                    ecbValutakursRestKlient.hentValutakurs(Frequency.Daily, listOf(ECBConstants.NOK, utenlandskValuta), kursDato)
                validateExchangeRates(utenlandskValuta, kursDato, valutakurser)
                val valutakursNOK = valutakurser.exchangeRateForCurrency(ECBConstants.NOK)!!
                val lagretValutakurs =
                    if (utenlandskValuta == ECBConstants.EUR) {
                        ecbValutakursCacheRepository.save(ECBValutakursCache(kurs = valutakursNOK.kurs, valutakode = utenlandskValuta, valutakursdato = kursDato))
                    } else {
                        val valutakursUtenlandskValuta = valutakurser.exchangeRateForCurrency(utenlandskValuta)!!
                        ecbValutakursCacheRepository.save(
                            ECBValutakursCache(
                                kurs = beregnValutakursINOK(valutakursUtenlandskValuta.kurs, valutakursNOK.kurs),
                                valutakode = utenlandskValuta,
                                valutakursdato = kursDato,
                            ),
                        )
                    }

                return lagretValutakurs.kurs
            } catch (e: ValutakursClientException) {
                throw ECBServiceException(e.message, e)
            }
        }
        logger.info("Valutakurs ble hentet fra cache for ${utenlandskValuta.saner()} på $kursDato")
        return valutakurs.kurs
    }

    private fun beregnValutakursINOK(
        valutakursUtenlandskValuta: BigDecimal,
        valutakursNOK: BigDecimal,
    ) = valutakursNOK.del(valutakursUtenlandskValuta, 10)

    private fun validateExchangeRates(
        currency: String,
        exchangeRateDate: LocalDate,
        exchangeRates: List<Valutakurs>,
    ) {
        val expectedSize = if (currency != ECBConstants.EUR) 2 else 1
        val currencies =
            if (currency != ECBConstants.EUR) listOf(currency, ECBConstants.NOK) else listOf(ECBConstants.NOK)

        if (!isValid(exchangeRates, currencies, exchangeRateDate, expectedSize)) {
            throwValidationException(currency, exchangeRateDate)
        }
    }

    private fun isValid(
        exchangeRates: List<Valutakurs>,
        currencies: List<String>,
        exchangeRateDate: LocalDate,
        expectedSize: Int,
    ) = exchangeRates.size == expectedSize &&
        exchangeRates.all { it.kursDato.isEqual(exchangeRateDate) } &&
        exchangeRates.map { it.valuta }.containsAll(currencies)

    private fun throwValidationException(
        currency: String,
        exchangeRateDate: LocalDate,
    ): Unit = throw ECBServiceException("Fant ikke nødvendige valutakurser for valutakursdato ${exchangeRateDate.tilKortString()} for å bestemme valutakursen $currency - NOK")
}

object ECBConstants {
    const val NOK = "NOK"
    const val EUR = "EUR"
}
