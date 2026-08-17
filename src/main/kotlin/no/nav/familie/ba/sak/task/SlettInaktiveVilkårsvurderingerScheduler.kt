package no.nav.familie.ba.sak.task

import no.nav.familie.ba.sak.config.LeaderClientService
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.SlettInaktiveVilkårsvurderingerService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Sletter gamle inaktive vilkårsvurderinger (aktiv = false) med tilhørende person_resultat,
 * vilkar_resultat og annen_vurdering. Vilkårsvurderinger som erstattes slettes nå fortløpende
 * (se VilkårsvurderingService.lagreNyOgSlettGammel); denne jobben rydder opp i radene som ble
 * deaktivert før den endringen. Ingenting i ba-sak skriver lenger aktiv = false, og ingenting
 * leser inaktive rader – ikke gjeninnfør deaktivering (ks-sak gjør det fortsatt), for slike rader
 * vil bli slettet av denne jobben.
 *
 * Når jobben logger at det ikke er flere igjen, kan følgende fjernes/gjøres (del 3):
 * - denne jobben, togglen SKAL_SLETTE_INAKTIVE_VILKÅRSVURDERINGER, SlettInaktiveVilkårsvurderingerService
 *   og de tilhørende spørringene i VilkårsvurderingRepository
 * - kolonnen vilkaarsvurdering.aktiv, `aktiv` i Vilkårsvurdering (inkl. kopier()), `AND v.aktiv = true`
 *   i VilkårsvurderingRepository.findByBehandlingAndAktiv og `aktiv`-parameteren i testdatageneratorene
 * - unik indeks på vilkaarsvurdering(fk_behandling_id), som er invarianten vi nå har: én vilkårsvurdering per behandling
 *
 * Merk: Bevisst ikke @Transactional her. Hver batch kjøres i egen transaksjon i
 * SlettInaktiveVilkårsvurderingerService.
 */
@Component
class SlettInaktiveVilkårsvurderingerScheduler(
    private val slettInaktiveVilkårsvurderingerService: SlettInaktiveVilkårsvurderingerService,
    private val featureToggleService: FeatureToggleService,
    private val leaderClientService: LeaderClientService,
) {
    @Scheduled(cron = "0 0 5 * * *")
    fun slettInaktiveVilkårsvurderinger() {
        if (!leaderClientService.isLeader()) return
        if (!featureToggleService.isEnabled(FeatureToggle.SKAL_SLETTE_INAKTIVE_VILKÅRSVURDERINGER)) return

        var totaltSlettet = 0
        var sisteSlettedeId = 0L
        for (batch in 0 until MAKS_ANTALL_BATCHER_PER_KJØRING) {
            val slettedeIder =
                slettInaktiveVilkårsvurderingerService.slettBatchMedInaktiveVilkårsvurderinger(
                    etterId = sisteSlettedeId,
                    batchStørrelse = BATCH_STØRRELSE,
                )
            if (slettedeIder.isEmpty()) break

            totaltSlettet += slettedeIder.size
            sisteSlettedeId = slettedeIder.last()
        }

        if (totaltSlettet > 0) {
            logger.info("Slettet $totaltSlettet inaktive vilkårsvurderinger med tilhørende personresultater, vilkårresultater og andre vurderinger")
        } else {
            logger.info("Fant ingen inaktive vilkårsvurderinger å slette. Jobben og togglen ${FeatureToggle.SKAL_SLETTE_INAKTIVE_VILKÅRSVURDERINGER} kan fjernes.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SlettInaktiveVilkårsvurderingerScheduler::class.java)
        private const val BATCH_STØRRELSE = 1000
        private const val MAKS_ANTALL_BATCHER_PER_KJØRING = 100
    }
}
