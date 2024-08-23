package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakStegService
import no.nav.familie.ba.sak.kjerne.autovedtak.OmregningBrevData
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.StartSatsendring
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.task.dto.AutobrevPgaAlderDTO
import no.nav.familie.prosessering.error.RekjørSenereException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class AutobrevOmregningPgaAlderService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val autovedtakBrevService: AutovedtakBrevService,
    private val autovedtakStegService: AutovedtakStegService,
    private val andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService,
    private val startSatsendring: StartSatsendring,
) {
    @Transactional
    fun opprettOmregningsoppgaveForBarnIBrytingsalder(
        autobrevPgaAlderDTO: AutobrevPgaAlderDTO,
        førstegangKjørt: LocalDateTime = LocalDateTime.now(),
    ): AutobrevOmregningSvar {
        logger.info("opprettOmregningsoppgaveForBarnIBrytingsalder for fagsak ${autobrevPgaAlderDTO.fagsakId}")

        val behandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(autobrevPgaAlderDTO.fagsakId)
                ?: error("Fant ikke aktiv behandling")

        val behandlingsårsak =
            finnBehandlingÅrsakForAlder(
                autobrevPgaAlderDTO.alder,
            )

        if (!autovedtakBrevService.skalAutobrevBehandlingOpprettes(
                fagsakId = autobrevPgaAlderDTO.fagsakId,
                behandlingsårsak = behandlingsårsak,
                standardbegrunnelser = AutobrevUtils.hentStandardbegrunnelserReduksjonForAlder(autobrevPgaAlderDTO.alder),
            )
        ) {
            return AutobrevOmregningSvar.HAR_ALT_SENDT
        }

        if (behandling.fagsak.status != FagsakStatus.LØPENDE) {
            logger.info("Fagsak ${behandling.fagsak.id} har ikke status løpende, og derfor prosesseres den ikke videre.")
            return AutobrevOmregningSvar.FAGSAK_IKKE_LØPENDE
        }

        if (!barnMedAngittAlderInneværendeMånedEksisterer(
                behandlingId = behandling.id,
                alder = autobrevPgaAlderDTO.alder,
            )
        ) {
            logger.warn("Fagsak ${behandling.fagsak.id} har ikke noe barn med alder ${autobrevPgaAlderDTO.alder} ")
            return AutobrevOmregningSvar.INGEN_BARN_I_ALDER
        }

        if (barnetrygdOpphører(autobrevPgaAlderDTO, behandling)) {
            logger.info("Fagsak ${behandling.fagsak.id} har ikke løpende utbetalinger for barn under 18 år og vil opphøre.")
            return AutobrevOmregningSvar.INGEN_LØPENDE_UTBETALING_FOR_BARN_UNDER_18
        }

        if (!barnIBrytningsalderHarLøpendeYtelse(
                alder = autobrevPgaAlderDTO.alder,
                behandlingId = behandling.id,
                årMåned = autobrevPgaAlderDTO.årMåned,
            )
        ) {
            logger.info("Ingen løpende ytelse for barnet i brytningsalder for fagsak=${behandling.fagsak.id} behandlingsårsak=$behandlingsårsak")
            return AutobrevOmregningSvar.INGEN_LØPENDE_YTELSE_FOR_BARN_I_BRYTNINGSALDER
        }

        if (startSatsendring.sjekkOgOpprettSatsendringVedGammelSats(autobrevPgaAlderDTO.fagsakId)) {
            throw RekjørSenereException(
                "Satsedring skal kjøre ferdig før man behandler autobrev 6 og 18 år",
                LocalDateTime.now().plusHours(1),
            )
        }

        if (erEØSMedNullutbetaling(
                alder = autobrevPgaAlderDTO.alder,
                behandling = behandling,
                årMåned = autobrevPgaAlderDTO.årMåned,
            )
        ) {
            logger.info("Sender ikke ut omregningsbrev for EØS med nullutbetaling for fagsak=${behandling.fagsak.id}")
            return AutobrevOmregningSvar.EØS_MED_NULLUTBETALING
        }

        autovedtakStegService.kjørBehandlingOmregning(
            mottakersAktør = behandling.fagsak.aktør,
            behandlingsdata =
                OmregningBrevData(
                    aktør = behandling.fagsak.aktør,
                    behandlingsårsak = behandlingsårsak,
                    standardbegrunnelse =
                        AutobrevUtils.hentGjeldendeVedtakbegrunnelseReduksjonForAlder(
                            autobrevPgaAlderDTO.alder,
                        ),
                    fagsakId = behandling.fagsak.id,
                ),
            førstegangKjørt = førstegangKjørt,
        )
        return AutobrevOmregningSvar.OK
    }

    private fun barnetrygdOpphører(
        autobrevPgaAlderDTO: AutobrevPgaAlderDTO,
        behandling: Behandling,
    ) =
        autobrevPgaAlderDTO.alder == Alder.ATTEN.år &&
            !barnUnder18årInneværendeMånedEksisterer(behandlingId = behandling.id)

    private fun finnBehandlingÅrsakForAlder(alder: Int): BehandlingÅrsak =
        when (alder) {
            Alder.ATTEN.år -> BehandlingÅrsak.OMREGNING_18ÅR
            else -> throw Feil("Alder må være oppgitt til 18 år.")
        }

    private fun barnMedAngittAlderInneværendeMånedEksisterer(
        behandlingId: Long,
        alder: Int,
    ): Boolean =
        barnMedAngittAlderInneværendeMåned(behandlingId, alder).isNotEmpty()

    private fun barnMedAngittAlderInneværendeMåned(
        behandlingId: Long,
        alder: Int,
    ): List<Person> =
        personopplysningGrunnlagRepository
            .findByBehandlingAndAktiv(behandlingId = behandlingId)
            ?.barna
            ?.filter { it.type == PersonType.BARN && it.fyllerAntallÅrInneværendeMåned(alder) }
            ?.toList() ?: listOf()

    private fun barnUnder18årInneværendeMånedEksisterer(behandlingId: Long): Boolean =
        personopplysningGrunnlagRepository
            .findByBehandlingAndAktiv(behandlingId = behandlingId)
            ?.barna
            ?.any { it.type == PersonType.BARN && it.erYngreEnnInneværendeMåned(Alder.ATTEN.år) } ?: false

    private fun barnIBrytningsalderHarLøpendeYtelse(
        alder: Int,
        behandlingId: Long,
        årMåned: YearMonth,
    ): Boolean {
        val andelerTilBarnIBrytningsalder =
            finnAndelerTilBarnIBrytningsalder(behandlingId, alder)

        return harBarnIBrytningsalderLøpendeAndeler(alder, andelerTilBarnIBrytningsalder, årMåned)
    }

    private fun harBarnIBrytningsalderLøpendeAndeler(
        alder: Int,
        andelerTilBarnIBrytningsalder: List<AndelTilkjentYtelseMedEndreteUtbetalinger>,
        årMåned: YearMonth,
    ) = when (alder) {
        Alder.ATTEN.år -> andelerTilBarnIBrytningsalder.any { it.stønadTom.plusMonths(1) == årMåned }
        else -> throw Feil("Ugyldig alder")
    }

    private fun erEØSMedNullutbetaling(
        alder: Int,
        behandling: Behandling,
        årMåned: YearMonth,
    ): Boolean {
        if (behandling.kategori != BehandlingKategori.EØS) {
            return false
        }
        val andelerTilBarnIBrytningsalder =
            finnAndelerTilBarnIBrytningsalder(behandling.id, alder)

        return when (alder) {
            Alder.ATTEN.år -> andelerTilBarnIBrytningsalder.any { it.stønadTom.plusMonths(1) == årMåned && it.erAndelSomharNullutbetalingPgaDifferanseberegning() }
            else -> false
        }
    }

    private fun finnAndelerTilBarnIBrytningsalder(
        behandlingId: Long,
        alder: Int,
    ): List<AndelTilkjentYtelseMedEndreteUtbetalinger> {
        val barnIBrytningsalder =
            barnMedAngittAlderInneværendeMåned(behandlingId = behandlingId, alder = alder).map { it.aktør }

        if (barnIBrytningsalder.isEmpty()) {
            throw Feil("Forventer å finne minst et barn i brytningsalder for omregning 6 eller 18 år for behandling=$behandlingId")
        }

        val andelerTilBarnIBrytningsalder =
            andelerTilkjentYtelseOgEndreteUtbetalingerService
                .finnAndelerTilkjentYtelseMedEndreteUtbetalinger(
                    behandlingId,
                ).filter { it.aktør in barnIBrytningsalder }
        return andelerTilBarnIBrytningsalder
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutobrevOmregningPgaAlderService::class.java)
    }
}

enum class Alder(
    val år: Int,
) {
    ATTEN(år = 18),
}

enum class AutobrevOmregningSvar {
    OK,
    FAGSAK_IKKE_LØPENDE,
    INGEN_BARN_I_ALDER,
    HAR_ALT_SENDT,
    INGEN_LØPENDE_UTBETALING_FOR_BARN_UNDER_18,
    EØS_MED_NULLUTBETALING,
    INGEN_LØPENDE_YTELSE_FOR_BARN_I_BRYTNINGSALDER,
}
