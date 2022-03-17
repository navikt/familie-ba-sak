package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.Autovedtaktype
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.task.dto.Autobrev6og18ÅrDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Autobrev6og18ÅrService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val behandlingService: BehandlingService,
    private val autovedtakBrevService: AutovedtakBrevService,
    private val autovedtakStegService: AutovedtakStegService
) {

    @Transactional
    fun opprettOmregningsoppgaveForBarnIBrytingsalder(autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO) {
        logger.info("opprettOmregningsoppgaveForBarnIBrytingsalder for fagsak ${autobrev6og18ÅrDTO.fagsakId}")

        val behandling =
            behandlingService.hentSisteBehandlingSomErVedtatt(autobrev6og18ÅrDTO.fagsakId)
                ?: error("Fant ikke aktiv behandling")

        val behandlingsårsak = finnBehandlingÅrsakForAlder(
            autobrev6og18ÅrDTO.alder
        )

        if (!autovedtakBrevService.skalAutobrevBehandlingOpprettes(
                fagsakId = autobrev6og18ÅrDTO.fagsakId,
                behandlingsårsak = behandlingsårsak,
                standardbegrunnelser = AutobrevUtils.hentStandardbegrunnelserReduksjonForAlder(autobrev6og18ÅrDTO.alder)
            )
        ) {
            return
        }

        if (behandling.fagsak.status != FagsakStatus.LØPENDE) {
            logger.info("Fagsak ${behandling.fagsak.id} har ikke status løpende, og derfor prosesseres den ikke videre.")
            return
        }

        if (!barnMedAngittAlderInneværendeMånedEksisterer(
                behandlingId = behandling.id,
                alder = autobrev6og18ÅrDTO.alder
            )
        ) {
            logger.warn("Fagsak ${behandling.fagsak.id} har ikke noe barn med alder ${autobrev6og18ÅrDTO.alder} ")
            return
        }

        if (barnetrygdOpphører(autobrev6og18ÅrDTO, behandling)) {
            logger.info("Fagsak ${behandling.fagsak.id} har ikke løpende utbetalinger for barn under 18 år og vil opphøre.")
            return
        }

        autovedtakStegService.kjørBehandling(
            mottakersAktør = behandling.fagsak.aktør,
            autovedtaktype = Autovedtaktype.OMREGNING_BREV,
            behandlingsdata = AutovedtakBrevBehandlingsdata(
                aktør = behandling.fagsak.aktør,
                behandlingsårsak = behandlingsårsak,
                standardbegrunnelse = AutobrevUtils.hentGjeldendeVedtakbegrunnelseReduksjonForAlder(
                    autobrev6og18ÅrDTO.alder
                )
            )
        )
    }

    private fun barnetrygdOpphører(
        autobrev6og18ÅrDTO: Autobrev6og18ÅrDTO,
        behandling: Behandling
    ) =
        autobrev6og18ÅrDTO.alder == Alder.ATTEN.år &&
            !barnUnder18årInneværendeMånedEksisterer(behandlingId = behandling.id)

    private fun finnBehandlingÅrsakForAlder(alder: Int): BehandlingÅrsak =
        when (alder) {
            Alder.SEKS.år -> BehandlingÅrsak.OMREGNING_6ÅR
            Alder.ATTEN.år -> BehandlingÅrsak.OMREGNING_18ÅR
            else -> throw Feil("Alder må være oppgitt til enten 6 eller 18 år.")
        }

    private fun barnMedAngittAlderInneværendeMånedEksisterer(behandlingId: Long, alder: Int): Boolean =
        barnMedAngittAlderInneværendeMåned(behandlingId, alder).isNotEmpty()

    private fun barnMedAngittAlderInneværendeMåned(behandlingId: Long, alder: Int): List<Person> =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.barna
            ?.filter { it.type == PersonType.BARN && it.fyllerAntallÅrInneværendeMåned(alder) }?.toList() ?: listOf()

    private fun barnUnder18årInneværendeMånedEksisterer(behandlingId: Long): Boolean =
        personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)?.barna
            ?.any { it.type == PersonType.BARN && it.erYngreEnnInneværendeMåned(Alder.ATTEN.år) } ?: false

    companion object {

        private val logger = LoggerFactory.getLogger(Autobrev6og18ÅrService::class.java)
    }
}

enum class Alder(val år: Int) {
    SEKS(år = 6),
    ATTEN(år = 18)
}
