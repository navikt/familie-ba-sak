package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene.FinnmarkstilleggKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.domene.FinnmarkstilleggKjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutovedtakFinnmarkstilleggTaskOppretter(
    private val fagsakRepository: FagsakRepository,
    private val opprettTaskService: OpprettTaskService,
    private val finnmarkstilleggKjøringRepository: FinnmarkstilleggKjøringRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val pdlRestClient: SystemOnlyPdlRestClient,
) {
    @Transactional
    fun opprettTasker(antallFagsaker: Int) {
        var antallBehandlingerStartet = 0
        var startSide = 0

        while (antallBehandlingerStartet < antallFagsaker) {
            val page = fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(Pageable.ofSize(antallFagsaker))
            val fagsakIder = page.toSet()

            val iverksatteBehandlinger =
                behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder).values

            val sistIverksatteBehandlingerUtenEøs = iverksatteBehandlinger.filter { it.kategori != BehandlingKategori.EØS }

            val grunnlagForIverksatteBehandlinger =
                persongrunnlagService.hentAktivForBehandlinger(sistIverksatteBehandlingerUtenEøs.map { it.id })

            val fagsakerMedPersonidenter =
                sistIverksatteBehandlingerUtenEøs.associate { behandling ->
                    val grunnlag = grunnlagForIverksatteBehandlinger[behandling.id]
                    if (grunnlag == null) {
                        throw Feil("Forventet personopplysningsgrunnlag for behandling ${behandling.id} ikke funnet.")
                    }
                    val fødselsnummer = grunnlag.personer.map { person -> person.aktør.aktivFødselsnummer() }
                    behandling.fagsak.id to fødselsnummer
                }

            val personerSomBorIFinnmarkEllerNordTroms =
                fagsakerMedPersonidenter.values
                    .flatten()
                    .distinct()
                    .chunked(1000)
                    .flatMap { personer ->
                        pdlRestClient
                            .hentBostedsadresseOgDeltBostedForPersoner(personer)
                            .mapValues { Adresser.opprettFra(it.value) }
                            .filterValues { it.harAdresserSomErRelevantForFinnmarkstillegg() }
                            .keys
                    }

            val fagsakerDerMinstEnAktørBorIFinnmarkEllerNordTroms =
                fagsakerMedPersonidenter
                    .filterValues { personerSomBorIFinnmarkEllerNordTroms.intersect(it).isNotEmpty() }
                    .keys

            val fagsakIderSomIkkeSkalOpprettesTaskFor = fagsakIder - fagsakerDerMinstEnAktørBorIFinnmarkEllerNordTroms
            finnmarkstilleggKjøringRepository.saveAll(fagsakIderSomIkkeSkalOpprettesTaskFor.map { FinnmarkstilleggKjøring(fagsakId = it) })

            opprettTaskService.opprettAutovedtakFinnmarkstilleggTasker(fagsakerDerMinstEnAktørBorIFinnmarkEllerNordTroms)

            antallBehandlingerStartet += fagsakerDerMinstEnAktørBorIFinnmarkEllerNordTroms.size

            if (++startSide >= page.totalPages) {
                break
            }
        }
    }
}
