package no.nav.familie.ba.sak.kjerne.eøs.sats

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.AutovedtakSkalIkkeGjennomføresFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsKjøringService
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.konverterBeløpTilMånedlig
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSatserRegister.hentSatsForLandIMåned
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtfyltUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.filtrerErUtfylt
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
                .filtrerErUtfylt()
                .filter { it.utbetalingsland == nySats.land }
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
     * Returnerer `true` hvis [utenlandskPeriodebeløp] ble oppdatert, `false` hvis perioden
     * ikke overlapper [nySats] eller beløpet allerede er lik [nySats].
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
        if (!utenlandskPeriodebeløp.overlapper(nySats)) {
            logger.info("UtenlandskPeriodebeløp ${utenlandskPeriodebeløp.id} overlapper ikke ny sats $nySats.")
            return false
        }
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

    /**
     * Validerer at [utenlandskPeriodebeløp] kan oppdateres automatisk til [nySats].
     *
     * @throws [AutovedtakMåBehandlesManueltFeil] hvis ett av følgende krav ikke er oppfylt:
     * - Beløp i [utenlandskPeriodebeløp] er lik beløp i [forrigeSats] (uendret siden forrige satsendring)
     * - Valuta i [utenlandskPeriodebeløp] er lik valuta i [nySats]
     * - Intervall i [utenlandskPeriodebeløp] er lik intervall i [nySats]
     */
    private fun validerAtUtenlandskPeriodebeløpKanOppdateresAutomatisk(
        utenlandskPeriodebeløp: UtfyltUtenlandskPeriodebeløp,
        forrigeSats: EøsSats,
        nySats: EøsSats,
    ) {
        val behandlingId = utenlandskPeriodebeløp.behandlingId
        val utbetalingsland = utenlandskPeriodebeløp.utbetalingsland
        val valutakode = utenlandskPeriodebeløp.valutakode
        val beløp = utenlandskPeriodebeløp.beløp
        val intervall = utenlandskPeriodebeløp.intervall

        if (forrigeSats.beløp.compareTo(beløp) != 0) {
            throw AutovedtakMåBehandlesManueltFeil(
                "UtenlandskPeriodebeløp for behandling $behandlingId og land $utbetalingsland " +
                    "har beløp $beløp $valutakode, mens forrige EØS-sats har beløp ${forrigeSats.beløp} ${forrigeSats.valuta}.",
            )
        }

        if (nySats.valuta != valutakode) {
            throw AutovedtakMåBehandlesManueltFeil(
                "UtenlandskPeriodebeløp for behandling $behandlingId og land $utbetalingsland " +
                    "har valuta $valutakode, mens ny EØS-sats har valuta ${nySats.valuta}.",
            )
        }

        if (nySats.intervall != intervall) {
            throw AutovedtakMåBehandlesManueltFeil(
                "UtenlandskPeriodebeløp for behandling $behandlingId og land $utbetalingsland " +
                    "har intervall $intervall, mens ny EØS-sats har intervall ${nySats.intervall}.",
            )
        }
    }

    private fun UtfyltUtenlandskPeriodebeløp.overlapper(eøsSats: EøsSats): Boolean =
        (this.tom == null || eøsSats.fom <= this.tom) &&
            (eøsSats.tom == null || this.fom <= eøsSats.tom)

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
