package no.nav.familie.ba.sak.kjerne.minside

import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.springframework.stereotype.Service

@Service
class InnvilgetBarnetrygdService(
    private val personidentService: PersonidentService,
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    fun hentInnvilgetBarnetrygd(ident: String): InnvilgetBarnetrygd {
        val aktør = personidentService.hentAktør(ident)

        val fagsak = fagsakService.hentFagsakPåPerson(aktør, FagsakType.NORMAL)
        if (fagsak == null) {
            return InnvilgetBarnetrygd.opprettIngenInnvilgetBarnetrygd()
        }

        val behandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsak.id)
        if (behandling == null) {
            return InnvilgetBarnetrygd.opprettIngenInnvilgetBarnetrygd()
        }

        val andeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        val ordinærStartmåned =
            andeler
                .filter { it.type == YtelseType.ORDINÆR_BARNETRYGD }
                .map { it.stønadFom }
                .minByOrNull { it }

        val utvidetStartmåned =
            andeler
                .filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
                .map { it.stønadFom }
                .minByOrNull { it }

        return InnvilgetBarnetrygd(
            ordinær = if (ordinærStartmåned != null) InnvilgetBarnetrygd.Ordinær(ordinærStartmåned) else null,
            utvidet = if (utvidetStartmåned != null) InnvilgetBarnetrygd.Utvidet(utvidetStartmåned) else null,
        )
    }
}
