package no.nav.familie.ba.sak.kjerne.fagsaklåsing

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.DokarkivBruker
import no.nav.familie.kontrakter.felles.dokarkiv.GjenåpneSakRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FagsakLåsingService(
    private val fagsakRepository: FagsakRepository,
    private val fagsakLåsingRepository: FagsakLåsingRepository,
    private val integrasjonKlient: IntegrasjonKlient,
) {
    @Transactional
    fun låsOppFagsak(
        fagsakId: Long,
        begrunnelseForÅLåseOppFagsak: String,
    ): Fagsak {
        val fagsak = fagsakRepository.finnFagsak(fagsakId) ?: throw Feil("Finner ikke fagsak med id $fagsakId")

        if (fagsak.status != FagsakStatus.LÅST) {
            throw FunksjonellFeil("Fagsaken må ha status LÅST for å kunne låses opp. Nåværende status: ${fagsak.status}")
        }

        if (begrunnelseForÅLåseOppFagsak.isBlank()) {
            throw FunksjonellFeil("Begrunnelse kan ikke være tom")
        }

        lagreOgDeaktiverGammel(
            FagsakLåsing(
                fagsak = fagsak,
                tidspunkt = LocalDateTime.now(),
                hendelse = FagsakLåsHendelse.LÅST_OPP,
                begrunnelse = begrunnelseForÅLåseOppFagsak,
                aktiv = true,
            ),
        )

        fagsak.status = FagsakStatus.AVSLUTTET
        fagsakRepository.save(fagsak)

        integrasjonKlient.gjenåpneSakIDokarkiv(
            GjenåpneSakRequest(
                tema = Tema.BAR,
                fagsakId = fagsakId.toString(),
                fagsaksystem = Fagsystem.BA,
                bruker =
                    DokarkivBruker(
                        idType = BrukerIdType.FNR,
                        id = fagsak.aktør.aktivFødselsnummer(),
                    ),
            ),
        )

        return fagsak
    }

    fun lagreOgDeaktiverGammel(fagsakLåsing: FagsakLåsing): FagsakLåsing {
        val aktivFagsakLåsing = fagsakLåsingRepository.finnAktivLåsForFagsak(fagsakLåsing.fagsak.id)

        if (aktivFagsakLåsing != null && aktivFagsakLåsing.id != fagsakLåsing.id) {
            fagsakLåsingRepository.saveAndFlush(aktivFagsakLåsing.also { it.aktiv = false })
        }

        return fagsakLåsingRepository.save(fagsakLåsing)
    }
}
