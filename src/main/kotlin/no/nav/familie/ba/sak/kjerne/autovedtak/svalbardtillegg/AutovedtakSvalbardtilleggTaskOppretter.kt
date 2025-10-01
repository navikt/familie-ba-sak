package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg

import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.domene.SvalbardtilleggKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.domene.SvalbardtilleggKjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class AutovedtakSvalbardtilleggTaskOppretter(
    private val fagsakRepository: FagsakRepository,
    private val opprettTaskService: OpprettTaskService,
    private val svalbardtilleggKjøringRepository: SvalbardtilleggKjøringRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val pdlRestClient: SystemOnlyPdlRestClient,
) {
    @Transactional
    fun opprettTasker(antallFagsaker: Int) {
        val fagsakIder =
            fagsakRepository.finnLøpendeFagsakerForSvalbardtilleggKjøring(Pageable.ofSize(antallFagsaker)).toSet()

        svalbardtilleggKjøringRepository.saveAll(fagsakIder.map { SvalbardtilleggKjøring(fagsakId = it) })

        val iverksatteBehandlinger =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder).values

        val sistIverksatteBehandlingerUtenEøs = iverksatteBehandlinger.filter { it.kategori != BehandlingKategori.EØS }

        val grunnlagForIverksatteBehandlinger =
            persongrunnlagService.hentAktivForBehandlinger(sistIverksatteBehandlingerUtenEøs.map { it.id })

        val fagsakerMedPersonidenter =
            sistIverksatteBehandlingerUtenEøs.associate { behandling ->
                val grunnlag = grunnlagForIverksatteBehandlinger[behandling.id]
                if (grunnlag == null) {
                    throw Feil("Forventet personopplysningsgrunnlag for behandling ${behandling.id} ikke funnet")
                }
                val fødselsnummer = grunnlag.personer.map { person -> person.aktør.aktivFødselsnummer() }
                behandling.fagsak.id to fødselsnummer
            }

        val personerSomBorPåSvalbard =
            fagsakerMedPersonidenter.values
                .flatten()
                .distinct()
                .chunked(1000)
                .flatMap { personer ->
                    pdlRestClient
                        .hentAdresserForPersoner(personer)
                        .mapValues { Adresser.opprettFra(it.value) }
                        .filterValues { it.harAdresserSomErRelevantForSvalbardtillegg() }
                        .keys
                }

        val fagsakerDerMinstEnAktørBorPåSvalbard =
            fagsakerMedPersonidenter
                .filterValues { personerSomBorPåSvalbard.intersect(it).isNotEmpty() }
                .keys

        opprettTaskService.opprettAutovedtakSvalbardtilleggTasker(fagsakerDerMinstEnAktørBorPåSvalbard)
    }
}
