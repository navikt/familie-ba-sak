package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringMottaker
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering

fun SimuleringMottaker.tilVedtakSimuleringMottaker(vedtak: Vedtak) =
        VedtakSimuleringMottaker(mottakerNummer = this.mottakerNummer,
                                 mottakerType = this.mottakerType,
                                 vedtak = vedtak)

fun SimulertPostering.tilVedtakSimuleringPostering(
        vedtakSimuleringMottaker: VedtakSimuleringMottaker) =
        VedtakSimuleringPostering(
                beløp = this.beløp,
                betalingType = this.betalingType,
                fagOmrådeKode = this.fagOmrådeKode,
                fom = this.fom,
                tom = this.tom,
                posteringType = this.posteringType,
                forfallsdato = this.forfallsdato,
                utenInntrekk = this.utenInntrekk,
                vedtakSimuleringMottaker = vedtakSimuleringMottaker,
        )

fun opprettSimuleringsobjekter(simuleringMottakere: List<SimuleringMottaker>,
                               vedtak: Vedtak): Pair<MutableList<VedtakSimuleringMottaker>, MutableList<VedtakSimuleringPostering>> {
    var vedtakSimuleringMottakere = mutableListOf<VedtakSimuleringMottaker>()
    var vedtakSimuleringPosteringer = mutableListOf<VedtakSimuleringPostering>()

    simuleringMottakere.forEach { simuleringMottaker ->
        val vedtakSimuleringMottaker = simuleringMottaker.tilVedtakSimuleringMottaker(vedtak)
        vedtakSimuleringMottakere.add(vedtakSimuleringMottaker)

        val simuleringPosteringer = simuleringMottaker.simulertPostering
        simuleringPosteringer.forEach { simulertPostering ->
            val vedtakSimuleringPostering = simulertPostering.tilVedtakSimuleringPostering(vedtakSimuleringMottaker)
            vedtakSimuleringPosteringer.add(vedtakSimuleringPostering)
        }
    }
    return Pair(vedtakSimuleringMottakere, vedtakSimuleringPosteringer)
}