package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.stream.Collectors

@Service
class SatsendringService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val fagsakRepository: FagsakRepository,
    private val personOpplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val satskjøringRepository: SatskjøringRepository,
) {
    private val logger = LoggerFactory.getLogger(SatsendringService::class.java)

    fun erFagsakOppdatertMedSisteSatser(fagsakId: Long): Boolean {
        val sisteVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId)

        return if (sisteVedtatteBehandling == null) {
            true
        } else {
            val personOpplysningGrunnlagSisteVedtatteBehandling =
                personOpplysningGrunnlagRepository.findByBehandlingAndAktiv(sisteVedtatteBehandling.id)
                    ?: throw Feil("Fant ikke persongrunnlag på behandling ${sisteVedtatteBehandling.id}")

            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(sisteVedtatteBehandling.id)
                .erOppdatertMedSisteSatser(personOpplysningGrunnlagSisteVedtatteBehandling)
        }
    }

    fun finnLøpendeFagsakerUtenSisteSats(callId: String) {
        MDC.put(MDCConstants.MDC_CALL_ID, callId)
        val fagsakerUtenSisteSats = mutableListOf<Long>()
        var slice: Slice<Long> = fagsakRepository.finnLøpendeFagsaker(PageRequest.of(0, 10000))
        val løpendeFagsaker: List<Long> = slice.getContent()
        fagsakerUtenSisteSats.addAll(
            løpendeFagsaker
                .parallelStream()
                .filter {
                    !erFagsakOppdatertMedSisteSatser(it)
                }.collect(
                    Collectors.toList(),
                ),
        )

        while (slice.hasNext()) {
            logger.info("Next slice")
            slice = fagsakRepository.finnLøpendeFagsaker(slice.nextPageable())
            fagsakerUtenSisteSats.addAll(
                slice
                    .get()
                    .toList()
                    .parallelStream()
                    .filter {
                        !erFagsakOppdatertMedSisteSatser(it)
                    }.collect(
                        Collectors.toList(),
                    ),
            )
        }
        logger.warn("Følgende saker mangler satsendring:")
        fagsakerUtenSisteSats.chunked(1000) {
            logger.warn("$it")
        }
    }

    fun finnUferdigeSatskjøringer(
        feiltyper: List<SatsendringSvar>,
        satsTidspunkt: YearMonth = StartSatsendring.hentAktivSatsendringstidspunkt(),
    ): List<Long> =
        feiltyper.flatMap { feiltype ->
            satskjøringRepository.finnPåFeilTypeOgFerdigTidNull(feiltype.name, satsTidspunkt).map { it.fagsakId }
        }

    fun slettSatskjøringer(
        fagsakIder: Set<Long>,
        satsTidspunkt: YearMonth = StartSatsendring.hentAktivSatsendringstidspunkt(),
    ): List<Long> {
        logger.info("Sletter satskjøringer for fagsakIder: $fagsakIder")
        val satskjøringer = satskjøringRepository.findBySatsTidspunktAndFagsakIdIn(satsTidspunkt, fagsakIder)

        val ferdigeSatskjøringer = satskjøringer.filter { it.ferdigTidspunkt != null }
        if (ferdigeSatskjøringer.isNotEmpty()) {
            throw Feil(
                "Det finnes en eller flere satskjøringer for fagsaker ${ferdigeSatskjøringer.map { it.fagsakId }} som er ferdige. Kan ikke slette.",
            )
        }

        satskjøringRepository.deleteAll(satskjøringer)
        return satskjøringer.map { it.fagsakId }
    }
}
