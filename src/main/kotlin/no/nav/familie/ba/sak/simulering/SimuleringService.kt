package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.common.assertGenerelleSuksessKriterier
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.stereotype.Service

@Service
class SimuleringService(
        private val simuleringKlient: SimuleringKlient,
        private val økonomiService: ØkonomiService
) {

    fun hentSimulering(vedtak: Vedtak): DetaljertSimuleringResultat {
        Result.runCatching {
            simuleringKlient.hentSimulering(økonomiService.genererUtbetalingsoppdrag(vedtak = vedtak,
                                                                                     saksbehandlerId = SikkerhetContext.hentSaksbehandler()
                                                                                             .take(8)))
        }
                .fold(
                        onSuccess = {
                            assertGenerelleSuksessKriterier(it.body)
                            return it.body?.data!!
                        },
                        onFailure = {
                            throw Exception("Henting av etterbetalingsbeløp fra simulering feilet", it)
                        }
                )
    }
}