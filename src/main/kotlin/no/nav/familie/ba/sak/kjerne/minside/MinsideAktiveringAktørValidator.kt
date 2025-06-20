package no.nav.familie.ba.sak.kjerne.minside

import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Component

@Component
class MinsideAktiveringAktørValidator(
    private val fagsakService: FagsakService,
) {
    fun kanAktivereMinsideForAktør(aktør: Aktør): Boolean {
        val fagsakerForAktør = fagsakService.hentAlleFagsakerForAktør(aktør = aktør)

        // Vi ønsker ikke å vise minside dersom aktør ikke har noen fagsaker.
        if (fagsakerForAktør.isEmpty()) {
            return false
        }

        // Vi ønsker ikke å vise minside dersom aktør kun har fagsaker av typene SKJERMET_BARN eller INSTITUSJON.
        // Svært lite sannsynlig at en aktør har flere fagsaker
        if (fagsakerForAktør.all { it.type in listOf(FagsakType.SKJERMET_BARN, FagsakType.INSTITUSJON) }) {
            return false
        }

        return true
    }
}
