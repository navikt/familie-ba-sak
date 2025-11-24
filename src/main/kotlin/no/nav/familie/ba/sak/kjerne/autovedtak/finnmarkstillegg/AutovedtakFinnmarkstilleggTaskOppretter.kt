package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.task.OpprettTaskService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutovedtakFinnmarkstilleggTaskOppretter(
    private val fagsakRepository: FagsakRepository,
    private val opprettTaskService: OpprettTaskService,
    private val finnmarkstilleggKjøringService: FinnmarkstilleggKjøringService,
    private val persongrunnlagService: PersongrunnlagService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val pdlRestKlient: SystemOnlyPdlRestKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun opprettTasker(antallFagsaker: Int) {
        var antallBehandlingerStartet = 0
        var startSide = 0

        while (antallBehandlingerStartet < antallFagsaker) {
            val page = fagsakRepository.finnLøpendeFagsakerForFinnmarkstilleggKjøring(Pageable.ofSize(antallFagsaker))
            val fagsakIder = page.toSet()

            logger.info("Hentet ${fagsakIder.size} fagsaker for vurdering av autovedtak finnmarkstillegg")

            val iverksatteBehandlinger =
                behandlingHentOgPersisterService.hentSisteBehandlingSomErIverksattForFagsaker(fagsakIder).values

            val sistIverksatteBehandlingerUtenEøs = iverksatteBehandlinger.filter { it.kategori != BehandlingKategori.EØS }

            logger.info("Av ${iverksatteBehandlinger.size} iverksatte behandlinger er ${sistIverksatteBehandlingerUtenEøs.size} nasjonal")

            val grunnlagForIverksatteBehandlinger =
                persongrunnlagService.hentAktivForBehandlinger(sistIverksatteBehandlingerUtenEøs.map { it.id })

            logger.info("Hentet personopplysningsgrunnlag for ${grunnlagForIverksatteBehandlinger.size} behandlinger")

            val fagsakerMedPersonidenter =
                sistIverksatteBehandlingerUtenEøs
                    .filter { behandling ->
                        val grunnlag = grunnlagForIverksatteBehandlinger[behandling.id]
                        if (grunnlag == null) {
                            logger.error("Forventet personopplysningsgrunnlag for behandling ${behandling.id} ikke funnet.")
                        }
                        grunnlag != null
                    }.associate { behandling ->
                        val grunnlag = grunnlagForIverksatteBehandlinger[behandling.id]!!
                        val fødselsnummer = grunnlag.personer.map { person -> person.aktør.aktivFødselsnummer() }
                        behandling.fagsak.id to fødselsnummer
                    }

            val personerSomBorIFinnmarkEllerNordTroms =
                fagsakerMedPersonidenter.values
                    .flatten()
                    .distinct()
                    .chunked(1000)
                    .flatMap { personer ->
                        pdlRestKlient
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
            finnmarkstilleggKjøringService.lagreFinnmarkstilleggkjøringer(fagsakIderSomIkkeSkalOpprettesTaskFor)

            fagsakerDerMinstEnAktørBorIFinnmarkEllerNordTroms.forEach { fagsakId ->
                opprettTaskService.opprettAutovedtakFinnmarkstilleggTask(fagsakId)
            }

            logger.info("Opprettet ${fagsakerDerMinstEnAktørBorIFinnmarkEllerNordTroms.size} tasker for autovedtak finnmarkstillegg")

            antallBehandlingerStartet += fagsakerDerMinstEnAktørBorIFinnmarkEllerNordTroms.size

            logger.info("Totalt opprettet $antallBehandlingerStartet/$antallFagsaker tasker for autovedtak finnmarkstillegg")

            if (++startSide >= page.totalPages) {
                break
            }
        }
    }
}
