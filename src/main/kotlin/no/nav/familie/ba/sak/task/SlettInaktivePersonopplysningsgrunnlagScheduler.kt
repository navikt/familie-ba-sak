package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.SlettInaktivePersonopplysningsgrunnlagService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Sletter gamle inaktive personopplysningsgrunnlag (aktiv = false) med tilhørende po_person og
 * registeropplysninger. Grunnlag som erstattes slettes nå fortløpende
 * (se PersonopplysningGrunnlagLagreService.lagreOgSlettGammelt); denne jobben rydder opp i radene
 * som ble deaktivert før den endringen. Ingenting i ba-sak skriver lenger aktiv = false, og
 * ingenting leser inaktive rader – ikke gjeninnfør deaktivering, slike rader vil bli slettet her.
 *
 * Sletter bevisst kun inaktive grunnlag der behandlingen også har et aktivt grunnlag. Rader som
 * bryter den invarianten logges som advarsel i stedet for å slettes.
 *
 * Når jobben logger at det ikke er flere igjen, kan følgende fjernes (del 3):
 * - denne jobben, togglen SKAL_SLETTE_INAKTIVE_PERSONOPPLYSNINGSGRUNNLAG,
 *   SlettInaktivePersonopplysningsgrunnlagService og de tre spørringene i repositoryet
 * - kolonnen gr_personopplysninger.aktiv, `aktiv` i PersonopplysningGrunnlag (inkl. toString),
 *   `AND gr.aktiv = true` i de fire spørringene i PersonopplysningGrunnlagRepository,
 *   `AND gr.aktiv = true` i PersonRepository:15 og PersonRepository:29,
 *   FagsakRepository:136 (JPQL) og FagsakRepository:272 (native SQL),
 *   og `aktiv`-parameteren i PersonopplysningGrunnlagGenerator
 * - UIDX_GR_PERSONOPPLYSNINGER_01, erstattes av unik indeks på gr_personopplysninger(fk_behandling_id)
 *
 * Merk: Bevisst ikke @Transactional her. Hver batch kjøres i egen transaksjon i servicen.
 */
@Component
class SlettInaktivePersonopplysningsgrunnlagScheduler(
    private val slettInaktivePersonopplysningsgrunnlagService: SlettInaktivePersonopplysningsgrunnlagService,
    private val featureToggleService: FeatureToggleService,
    private val leaderClientService: LeaderClientService,
) {
    @Scheduled(cron = "0 0 3 * * *")
    fun slettInaktivePersonopplysningsgrunnlag() {
        if (!leaderClientService.isLeader()) return
        if (!featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_INAKTIVE_PERSONOPPLYSNINGSGRUNNLAG)) return

        var totaltSlettet = 0
        var sisteSlettedeId = 0L
        for (batch in 0 until MAKS_ANTALL_BATCHER_PER_KJØRING) {
            val slettedeIder =
                slettInaktivePersonopplysningsgrunnlagService.slettBatchMedInaktiveGrunnlag(
                    etterId = sisteSlettedeId,
                    batchStørrelse = BATCH_STØRRELSE,
                )
            if (slettedeIder.isEmpty()) break

            totaltSlettet += slettedeIder.size
            sisteSlettedeId = slettedeIder.last()
        }

        if (totaltSlettet > 0) {
            logger.info("Slettet $totaltSlettet inaktive personopplysningsgrunnlag med tilhørende personer og registeropplysninger")
        } else {
            val antallInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling = slettInaktivePersonopplysningsgrunnlagService.tellInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling()
            if (antallInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling > 0) {
                logger.warn(
                    "Fant $antallInaktiveGrunnlagUtenAktivtGrunnlagPåSammeBehandling inaktive personopplysningsgrunnlag på behandlinger uten aktivt grunnlag. " +
                        "Disse slettes ikke. Må undersøkes manuelt.",
                )
            } else {
                logger.info(
                    "Fant ingen inaktive personopplysningsgrunnlag å slette. " +
                        "Jobben og togglen ${FeatureToggle.SKAL_SLETTE_INAKTIVE_PERSONOPPLYSNINGSGRUNNLAG} kan fjernes.",
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SlettInaktivePersonopplysningsgrunnlagScheduler::class.java)
        internal const val BATCH_STØRRELSE = 200
        internal const val MAKS_ANTALL_BATCHER_PER_KJØRING = 50
    }
}
