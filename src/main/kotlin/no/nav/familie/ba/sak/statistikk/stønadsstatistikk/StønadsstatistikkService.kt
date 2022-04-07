package no.nav.familie.ba.sak.statistikk.stønadsstatistikk

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.beregnUtbetalingsperioderUtenKlassifisering
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.filtrerGjeldendeNå
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.eksterne.kontrakter.BehandlingOpprinnelse
import no.nav.familie.eksterne.kontrakter.BehandlingType
import no.nav.familie.eksterne.kontrakter.BehandlingÅrsak
import no.nav.familie.eksterne.kontrakter.Kategori
import no.nav.familie.eksterne.kontrakter.PersonDVH
import no.nav.familie.eksterne.kontrakter.Underkategori
import no.nav.familie.eksterne.kontrakter.UtbetalingsDetaljDVH
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVH
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.util.UUID

@Service
class StønadsstatistikkService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val persongrunnlagService: PersongrunnlagService,
    private val beregningService: BeregningService,
    private val vedtakService: VedtakService,
    private val personopplysningerService: PersonopplysningerService,
    private val vedtakRepository: VedtakRepository
) {

    fun hentVedtak(behandlingId: Long): VedtakDVH {

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val vedtak = vedtakService.hentAktivForBehandling(behandlingId)
        // DVH ønsker tidspunkt med klokkeslett

        var datoVedtak = vedtak?.vedtaksdato

        if (datoVedtak == null) {
            datoVedtak = vedtakRepository.finnVedtakForBehandling(behandlingId).singleOrNull()?.vedtaksdato
                ?: error("Fant ikke vedtaksdato for behandling $behandlingId")
        }

        val tidspunktVedtak = datoVedtak

        return VedtakDVH(
            fagsakId = behandling.fagsak.id.toString(),
            behandlingsId = behandlingId.toString(),
            tidspunktVedtak = tidspunktVedtak.atZone(TIMEZONE),
            person = hentSøker(behandlingId),
            ensligForsørger = utledEnsligForsørger(behandlingId), // TODO implementere støtte for dette
            kategori = Kategori.valueOf(behandling.kategori.name),
            underkategori = Underkategori.valueOf(behandling.underkategori.name),
            behandlingType = BehandlingType.valueOf(behandling.type.name),
            behandlingOpprinnelse = when (behandling.opprettetÅrsak) {
                no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.SØKNAD -> BehandlingOpprinnelse.MANUELL
                no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak.FØDSELSHENDELSE -> BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE
                else -> BehandlingOpprinnelse.MANUELL
            },
            utbetalingsperioder = hentUtbetalingsperioder(behandlingId),
            funksjonellId = UUID.randomUUID().toString(),
            behandlingÅrsak = BehandlingÅrsak.valueOf(behandling.opprettetÅrsak.name)
        )
    }

    private fun hentSøker(behandlingId: Long): PersonDVH {
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId)
        val søker = persongrunnlag.søker
        return lagPersonDVH(søker)
    }

    private fun hentUtbetalingsperioder(behandlingId: Long): List<UtbetalingsperiodeDVH> {

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId)

        if (tilkjentYtelse.andelerTilkjentYtelse.isEmpty()) return emptyList()

        val utbetalingsPerioder = beregnUtbetalingsperioderUtenKlassifisering(tilkjentYtelse.andelerTilkjentYtelse)

        return utbetalingsPerioder.toSegments()
            .sortedWith(compareBy<LocalDateSegment<Int>>({ it.fom }, { it.value }, { it.tom }))
            .map { segment ->
                val andelerForSegment = tilkjentYtelse.andelerTilkjentYtelse.filter {
                    segment.localDateInterval.overlaps(
                        LocalDateInterval(
                            it.stønadFom.førsteDagIInneværendeMåned(),
                            it.stønadTom.sisteDagIInneværendeMåned()
                        )
                    )
                }
                mapTilUtbetalingsperiode(
                    segment,
                    andelerForSegment,
                    tilkjentYtelse.behandling,
                    persongrunnlag
                )
            }
    }

    private fun utledEnsligForsørger(behandlingId: Long): Boolean {

        val tilkjentYtelse = beregningService.hentTilkjentYtelseForBehandling(behandlingId)
        if (tilkjentYtelse.andelerTilkjentYtelse.isEmpty()) return false

        return tilkjentYtelse.andelerTilkjentYtelse.find { it.type == YtelseType.UTVIDET_BARNETRYGD } != null
    }

    private fun mapTilUtbetalingsperiode(
        segment: LocalDateSegment<Int>,
        andelerForSegment: List<AndelTilkjentYtelse>,
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ): UtbetalingsperiodeDVH {
        return UtbetalingsperiodeDVH(
            hjemmel = "Ikke implementert",
            stønadFom = segment.fom,
            stønadTom = segment.tom,
            utbetaltPerMnd = segment.value,
            utbetalingsDetaljer = andelerForSegment.filter { it.erAndelSomSkalSendesTilOppdrag() }.map { andel ->
                val personForAndel =
                    personopplysningGrunnlag.søkerOgBarn.find { person -> andel.aktør == person.aktør }
                        ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")
                UtbetalingsDetaljDVH(
                    person = lagPersonDVH(
                        personForAndel,
                        andel.prosent.intValueExact()
                    ),
                    klassekode = andel.type.klassifisering,
                    utbetaltPrMnd = andel.kalkulertUtbetalingsbeløp,
                    delytelseId = behandling.fagsak.id.toString() + andel.periodeOffset
                )
            }
        )
    }

    private fun lagPersonDVH(person: Person, delingsProsentYtelse: Int = 0): PersonDVH {
        return PersonDVH(
            rolle = person.type.name,
            statsborgerskap = hentStatsborgerskap(person),
            bostedsland = hentLandkode(person),
            primærland = "IKKE IMPLMENTERT", // EØS
            sekundærland = "IKKE IMPLEMENTERT", // EØS
            delingsprosentOmsorg = 0, // Kan kanskje fjernes. Diskusjon på slack: Jeg tipper vi ikke trenger å sende den, men at det var noe vi “kladdet ned”, sikkert i en diskusjon om hvorvidt den faktiske delingsprosenten på omsorgen kan være ulik delingsprosenten på ytelsen
            delingsprosentYtelse = if (delingsProsentYtelse == 50) delingsProsentYtelse else 0,
            annenpartBostedsland = "Ikke implementert",
            annenpartPersonident = "ikke implementert",
            annenpartStatsborgerskap = "ikke implementert",
            personIdent = person.aktør.aktivFødselsnummer()
        )
    }

    private fun hentStatsborgerskap(person: Person): List<String> {
        return if (person.statsborgerskap.isNotEmpty()) person.statsborgerskap.filtrerGjeldendeNå().map { it.landkode }
        else listOf(personopplysningerService.hentGjeldendeStatsborgerskap(person.aktør).land)
    }

    private fun hentLandkode(person: Person): String = if (person.bostedsadresser.isNotEmpty()) "NO"
    else if (personopplysningerService.hentPersoninfoEnkel(person.aktør).bostedsadresser.isNotEmpty()) "NO" else {

        val landKode = personopplysningerService.hentLandkodeUtenlandskBostedsadresse(person.aktør)

        if (landKode == PersonopplysningerService.UKJENT_LANDKODE) {
            logger.info("Sender landkode ukjent til DVH")
            secureLogger.info("Ukjent land sendt til DVH for person ${person.aktør.aktivFødselsnummer()}")
        }
        landKode
    }

    companion object {

        private val logger = LoggerFactory.getLogger(StønadsstatistikkService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        private val TIMEZONE = ZoneId.of("Europe/Paris")
    }
}
