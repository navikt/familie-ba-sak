package no.nav.familie.ba.sak.kjerne.eøs.sats

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.AutovedtakSkalIkkeGjennomføresFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsKjøringService
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.konverterBeløpTilMånedlig
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSatserRegister.hentSatsForLandIMåned
import no.nav.familie.ba.sak.kjerne.eøs.sats.SatsendringEøsValidering.validerAtUtenlandskPeriodebeløpKanOppdateresAutomatisk
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtfyltUtenlandskPeriodebeløp
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SatsendringEøsService(
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val satsendringEøsKjøringService: SatsendringEøsKjøringService,
) {
    /**
     * Oppdaterer alle [UtenlandskPeriodebeløp] på [behandlingId] der
     * [utbetalingsland][UtenlandskPeriodebeløp.utbetalingsland] matcher utbetalingslandet
     * registrert i den tilhørende [SatsendringEøsKjøringService]-kjøringen.
     *
     * @throws [Feil] hvis ingen kjøring er registrert for [behandlingId], eller hvis ny sats
     *   eller forrige sats ikke finnes for utbetalingslandet.
     * @throws [AutovedtakSkalIkkeGjennomføresFeil] hvis ingen [UtenlandskPeriodebeløp] ble
     *   oppdatert — f.eks. fordi alle perioder allerede har ny sats eller ikke overlapper den.
     */
    @Transactional
    fun oppdaterUtenlandskPeriodebeløpMedSisteSats(behandlingId: BehandlingId) {
        val kjøring = satsendringEøsKjøringService.hentSatsendringEøsKjøring(behandlingId.id)
        val nySats = hentSatsForLandIMåned(kjøring.utbetalingsland, kjøring.satsTidspunkt)
        val forrigeSats = hentSatsForLandIMåned(kjøring.utbetalingsland, nySats.fom.minusMonths(1))

        val antallOppdaterteUtenlandskPeriodebeløp =
            utenlandskPeriodebeløpService
                .hentUtenlandskePeriodebeløp(behandlingId)
                .filtrerErRelevantForSats(nySats)
                .filter { oppdaterMedNySats(it, forrigeSats, nySats) }
                .size

        if (antallOppdaterteUtenlandskPeriodebeløp == 0) {
            throw AutovedtakSkalIkkeGjennomføresFeil(
                "Ingen UtenlandskPeriodebeløp trenger oppdatering for behandling ${behandlingId.id}.",
            )
        }
    }

    /**
     * Forsøker å oppdatere [utenlandskPeriodebeløp] fra [forrigeSats] til [nySats].
     *
     * Returnerer `true` hvis [utenlandskPeriodebeløp] ble oppdatert, `false` hvis
     * beløpet allerede er lik [nySats].
     *
     * @throws [AutovedtakMåBehandlesManueltFeil] via
     *   [validerAtUtenlandskPeriodebeløpKanOppdateresAutomatisk] hvis periodebeløpet ikke kan
     *   oppdateres automatisk.
     */
    private fun oppdaterMedNySats(
        utenlandskPeriodebeløp: UtfyltUtenlandskPeriodebeløp,
        forrigeSats: EøsSats,
        nySats: EøsSats,
    ): Boolean {
        if (nySats.beløp.compareTo(utenlandskPeriodebeløp.beløp) == 0) {
            logger.info("UtenlandskPeriodebeløp ${utenlandskPeriodebeløp.id} er allerede oppdatert med ny sats $nySats.")
            return false
        }

        validerAtUtenlandskPeriodebeløpKanOppdateresAutomatisk(utenlandskPeriodebeløp, forrigeSats, nySats)

        val nyttUtenlandskPeriodebeløp =
            UtenlandskPeriodebeløp(
                fom = maxOf(nySats.fom, utenlandskPeriodebeløp.fom),
                tom = utenlandskPeriodebeløp.tom,
                barnAktører = utenlandskPeriodebeløp.barnAktører,
                beløp = nySats.beløp,
                valutakode = nySats.valuta,
                intervall = nySats.intervall,
                utbetalingsland = utenlandskPeriodebeløp.utbetalingsland,
                kalkulertMånedligBeløp = nySats.intervall.konverterBeløpTilMånedlig(nySats.beløp),
            )

        utenlandskPeriodebeløpService.oppdaterUtenlandskPeriodebeløp(BehandlingId(utenlandskPeriodebeløp.behandlingId), nyttUtenlandskPeriodebeløp)

        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
