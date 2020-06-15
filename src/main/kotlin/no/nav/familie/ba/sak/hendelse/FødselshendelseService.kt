package no.nav.familie.ba.sak.hendelse

import no.nav.familie.ba.sak.behandling.fagsak.FagsakRepository
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.infotrygd.InfotrygdFeedService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import org.springframework.stereotype.Service

@Service
class FødselshendelseService(private val infotrygdFeedService: InfotrygdFeedService,
                             private val featureToggleService: FeatureToggleService,
                             private val fagsakRepository: FagsakRepository) {

    fun fødselshendelseSkalBehandlesHosInfotrygd(søkersIdent: String): Boolean {
        // TODO: Avgjør om fødsel skal behandles i BA-sak eller infotrygd basert på data fra replikatjenesten og BA-sak

        // Siden vi sender til ba-sak uansett, dersom søker har sak i ba-sak eller ikke har en sak i noen av fagsystemene,
        // holder det å sjekke om søker har en sak i infotrygd for å avgjøre hvor vi skal sende hendelsen videre.

        // Vi kunne slik sett ha droppet å sjekke databasen i ba-sak på dette stadiet, men fordi søker (feilaktig) kan finnes i
        // begge systemer, bør vi sjekke dette og kaste en feil på dette tidspunktet.

        // Tjenesten mot infotrygd-replika sjekker om søker eller barn finnes I DET HELE TATT, m.a.o. alle tidligere og
        // avsluttede søknader.

        // Det går an å slå opp barnets ident i infotrygd-replikaen også, men TEA-1332 spesifiserer kun søker (mor). Er
        // dette tilsiktet?
        val søkerHarEllerHarHattSakIInfotrygd = false

        val søkerHarEllerHarHattSakIBaSak = fagsakRepository.finnFagsakForPersonIdent(PersonIdent(søkersIdent)) == null

        return søkerHarEllerHarHattSakIInfotrygd || featureToggleService.isEnabled("familie-ba-sak.rollback-automatisk-regelkjoring")
    }

    fun sendTilInfotrygdFeed(barnIdenter: List<String>) {
        infotrygdFeedService.sendTilInfotrygdFeed(barnIdenter)
    }
}