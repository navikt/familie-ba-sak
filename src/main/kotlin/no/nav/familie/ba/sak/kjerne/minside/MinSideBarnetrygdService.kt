package no.nav.familie.ba.sak.kjerne.minside

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.Fødselsnummer
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class MinSideBarnetrygdService(
    private val personidentService: PersonidentService,
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val clockProvider: ClockProvider,
) {
    fun hentMinSideBarnetrygd(fødselsnummer: Fødselsnummer): MinSideBarnetrygd? {
        val aktør = personidentService.hentAktør(fødselsnummer.verdi)

        val fagsak = fagsakService.hentFagsakPåPerson(aktør, FagsakType.NORMAL)
        if (fagsak == null) {
            return null
        }

        val behandling = behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksatt(fagsak.id)
        if (behandling == null) {
            return null
        }

        val andeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandling.id)

        val harLøpendeOrdinærAndel =
            andeler
                .filter { it.type == YtelseType.ORDINÆR_BARNETRYGD }
                .any { it.stønadTom.isSameOrAfter(YearMonth.now(clockProvider.get())) }

        val harLøpendeUtvidetAndel =
            andeler
                .filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
                .any { it.stønadTom.isSameOrAfter(YearMonth.now(clockProvider.get())) }

        val ordinær =
            if (harLøpendeOrdinærAndel) {
                andeler
                    .filter { it.type == YtelseType.ORDINÆR_BARNETRYGD }
                    .map { it.stønadFom }
                    .minBy { it }
                    .let { MinSideBarnetrygd.Ordinær(it) }
            } else {
                null
            }

        val utvidet =
            if (harLøpendeUtvidetAndel) {
                andeler
                    .filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
                    .map { it.stønadFom }
                    .minBy { it }
                    .let { MinSideBarnetrygd.Utvidet(it) }
            } else {
                null
            }

        return MinSideBarnetrygd(ordinær, utvidet)
    }
}
