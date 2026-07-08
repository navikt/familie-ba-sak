package no.nav.familie.ba.sak.kjerne.eøs.sats

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsSvar.SATSENDRING_EØS_MÅ_BEHANDLES_MANUELT
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtfyltUtenlandskPeriodebeløp

object SatsendringEøsValidering {
    /**
     * Validerer at [utenlandskPeriodebeløp] kan oppdateres automatisk til [nySats].
     *
     * @throws [no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil] hvis ett av følgende krav ikke er oppfylt:
     * - Beløp i [utenlandskPeriodebeløp] er lik beløp i [forrigeSats] (uendret siden forrige satsendring)
     * - Valuta i [utenlandskPeriodebeløp] er lik valuta i [nySats]
     * - Intervall i [utenlandskPeriodebeløp] er lik intervall i [nySats]
     */
    fun validerAtUtenlandskPeriodebeløpKanOppdateresAutomatisk(
        utenlandskPeriodebeløp: UtfyltUtenlandskPeriodebeløp,
        forrigeSats: EøsSats,
        nySats: EøsSats,
    ) {
        val harAvvikIBeløp = forrigeSats.beløp.compareTo(utenlandskPeriodebeløp.beløp) != 0
        val harAvvikIValuta = nySats.valuta != utenlandskPeriodebeløp.valutakode
        val harAvvikIIntervall = nySats.intervall != utenlandskPeriodebeløp.intervall

        if (harAvvikIBeløp || harAvvikIValuta || harAvvikIIntervall) {
            throw AutovedtakMåBehandlesManueltFeil(SATSENDRING_EØS_MÅ_BEHANDLES_MANUELT.melding)
        }
    }
}
