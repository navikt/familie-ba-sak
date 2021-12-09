package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.task.dto.AutobrevOpphørOvergangsstonadDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OpphørAvFullOvergangsstonadService(
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val autovedtakService: AutovedtakService
) {
    @Transactional
    fun opprettOmregningsoppgavePgaOpphørtOvergangsstønadInneværendeMåned(asdf: AutobrevOpphørOvergangsstonadDTO) {
        println("kjør noen sjekker på at automatisk behandling faktisk skal kjøres")


        println("Kjør automatisk behandling")
    }
}
