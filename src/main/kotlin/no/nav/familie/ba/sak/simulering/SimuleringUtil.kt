package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringMottaker
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering

fun SimuleringMottaker.tilVedtakSimuleringMottaker(vedtak: Vedtak): VedtakSimuleringMottaker {
    val vedtakSimuleringMottaker = VedtakSimuleringMottaker(
            mottakerNummer = this.mottakerNummer,
            mottakerType = this.mottakerType,
            vedtak = vedtak,
    )

    vedtakSimuleringMottaker.vedtakSimuleringPostering = this.simulertPostering.map {
        it.tilVedtakSimuleringPostering(vedtakSimuleringMottaker)
    }

    return vedtakSimuleringMottaker
}

fun SimulertPostering.tilVedtakSimuleringPostering(vedtakSimuleringMottaker: VedtakSimuleringMottaker) =
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