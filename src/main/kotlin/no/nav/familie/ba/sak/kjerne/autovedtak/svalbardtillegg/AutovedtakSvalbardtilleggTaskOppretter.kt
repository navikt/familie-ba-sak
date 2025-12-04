package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg

import jakarta.transaction.Transactional
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.domene.SvalbardtilleggKjøring
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.domene.SvalbardtilleggKjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class AutovedtakSvalbardtilleggTaskOppretter(
    private val fagsakRepository: FagsakRepository,
    private val opprettTaskService: OpprettTaskService,
    private val svalbardtilleggKjøringRepository: SvalbardtilleggKjøringRepository,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingRepository: BehandlingRepository,
    private val pdlRestKlient: SystemOnlyPdlRestKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun opprettTasker(antallFagsaker: Int) {
        val fagsakIder =
            fagsakRepository.finnLøpendeFagsakerForSvalbardtilleggKjøring(Pageable.ofSize(antallFagsaker)).toSet()

        logger.info("Hentet ${fagsakIder.size} fagsaker for vurdering av autovedtak Svalbardtillegg")

        val iverksatteBehandlinger = behandlingRepository.finnSisteIverksatteBehandlingForFagsakerAndKategori(fagsakIder)
        val sistIverksatteBehandlingerUtenEøs = iverksatteBehandlinger.filter { it.kategori != BehandlingKategori.EØS.name }

        logger.info("Av ${iverksatteBehandlinger.size} iverksatte behandlinger er ${sistIverksatteBehandlingerUtenEøs.size} nasjonal")

        val grunnlagForIverksatteBehandlinger =
            persongrunnlagService.hentAktivForBehandlinger(sistIverksatteBehandlingerUtenEøs.map { it.behandlingId })

        logger.info("Hentet personopplysningsgrunnlag for ${grunnlagForIverksatteBehandlinger.size} behandlinger")

        val fagsakerMedPersonidenter =
            sistIverksatteBehandlingerUtenEøs
                .filter { behandling ->
                    val grunnlag = grunnlagForIverksatteBehandlinger[behandling.behandlingId]
                    if (grunnlag == null) {
                        logger.error("Forventet personopplysningsgrunnlag for behandling ${behandling.behandlingId} ikke funnet.")
                    }
                    grunnlag != null
                }.associate { behandling ->
                    val grunnlag = grunnlagForIverksatteBehandlinger[behandling.behandlingId]!!
                    val fødselsnummer = grunnlag.personer.map { person -> person.aktør.aktivFødselsnummer() }
                    behandling.fagsakId to fødselsnummer
                }

        val personerSomBorPåSvalbard =
            fagsakerMedPersonidenter.values
                .flatten()
                .distinct()
                .chunked(1000)
                .flatMap { personer ->
                    pdlRestKlient
                        .hentAdresserForPersoner(personer)
                        .mapValues { Adresser.opprettFra(it.value) }
                        .filterValues { it.harAdresserSomErRelevantForSvalbardtillegg() }
                        .keys
                }

        val fagsakerDerMinstEnAktørBorPåSvalbard =
            fagsakerMedPersonidenter
                .filterValues { personerSomBorPåSvalbard.intersect(it).isNotEmpty() }
                .keys

        val fagsakIderSomIkkeSkalOpprettesTaskFor = fagsakIder - fagsakerDerMinstEnAktørBorPåSvalbard
        svalbardtilleggKjøringRepository.saveAll(fagsakIderSomIkkeSkalOpprettesTaskFor.map { SvalbardtilleggKjøring(fagsakId = it) })

        opprettTaskService.opprettAutovedtakSvalbardtilleggTasker(fagsakerDerMinstEnAktørBorPåSvalbard)
        logger.info("Totalt opprettet ${fagsakerDerMinstEnAktørBorPåSvalbard.size}/$antallFagsaker tasker for autovedtak Svalbardtillegg")
    }
}
