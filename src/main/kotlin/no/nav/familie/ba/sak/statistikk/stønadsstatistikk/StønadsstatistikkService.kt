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
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.filtrerGjeldendeNå
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.eksterne.kontrakter.AnnenForeldersAktivitet
import no.nav.familie.eksterne.kontrakter.BehandlingOpprinnelse
import no.nav.familie.eksterne.kontrakter.BehandlingType
import no.nav.familie.eksterne.kontrakter.BehandlingTypeV2
import no.nav.familie.eksterne.kontrakter.BehandlingÅrsak
import no.nav.familie.eksterne.kontrakter.BehandlingÅrsakV2
import no.nav.familie.eksterne.kontrakter.Kategori
import no.nav.familie.eksterne.kontrakter.KategoriV2
import no.nav.familie.eksterne.kontrakter.Kompetanse
import no.nav.familie.eksterne.kontrakter.KompetanseResultat
import no.nav.familie.eksterne.kontrakter.PersonDVH
import no.nav.familie.eksterne.kontrakter.PersonDVHV2
import no.nav.familie.eksterne.kontrakter.SøkersAktivitet
import no.nav.familie.eksterne.kontrakter.Underkategori
import no.nav.familie.eksterne.kontrakter.UnderkategoriV2
import no.nav.familie.eksterne.kontrakter.UtbetalingsDetaljDVH
import no.nav.familie.eksterne.kontrakter.UtbetalingsDetaljDVHV2
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVH
import no.nav.familie.eksterne.kontrakter.UtbetalingsperiodeDVHV2
import no.nav.familie.eksterne.kontrakter.VedtakDVH
import no.nav.familie.eksterne.kontrakter.VedtakDVHV2
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
    private val vedtakRepository: VedtakRepository,
    private val kompetanseService: KompetanseService
) {

    fun hentVedtakV2(behandlingId: Long): VedtakDVHV2 {

        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        val vedtak = vedtakService.hentAktivForBehandling(behandlingId)
        // DVH ønsker tidspunkt med klokkeslett

        var datoVedtak = vedtak?.vedtaksdato

        if (datoVedtak == null) {
            datoVedtak = vedtakRepository.finnVedtakForBehandling(behandlingId).singleOrNull()?.vedtaksdato
                ?: error("Fant ikke vedtaksdato for behandling $behandlingId")
        }

        val tidspunktVedtak = datoVedtak

        return VedtakDVHV2(
            fagsakId = behandling.fagsak.id.toString(),
            behandlingsId = behandlingId.toString(),
            tidspunktVedtak = tidspunktVedtak.atZone(TIMEZONE),
            personV2 = hentSøkerV2(behandlingId),
            ensligForsørger = utledEnsligForsørger(behandlingId), // TODO implementere støtte for dette
            kategoriV2 = KategoriV2.valueOf(behandling.kategori.name),
            underkategoriV2 = UnderkategoriV2.valueOf(behandling.underkategori.name),
            behandlingTypeV2 = BehandlingTypeV2.valueOf(behandling.type.name),
            utbetalingsperioderV2 = hentUtbetalingsperioderV2(behandlingId),
            funksjonellId = UUID.randomUUID().toString(),
            kompetanseperioder = hentKompetanse(BehandlingId(behandlingId)),
            behandlingÅrsakV2 = BehandlingÅrsakV2.valueOf(behandling.opprettetÅrsak.name)
        )
    }

    private fun hentKompetanse(behandlingId: BehandlingId): List<Kompetanse> {
        val kompetanser = kompetanseService.hentKompetanser(behandlingId)

        return kompetanser.map { kompetanse ->
            Kompetanse(
                barnsIdenter = kompetanse.barnAktører.map { aktør -> aktør.aktivFødselsnummer() },
                annenForeldersAktivitet = AnnenForeldersAktivitet.valueOf(kompetanse.annenForeldersAktivitet!!.name),
                annenForeldersAktivitetsland = kompetanse.annenForeldersAktivitetsland,
                barnetsBostedsland = kompetanse.barnetsBostedsland,
                fom = kompetanse.fom!!,
                tom = kompetanse.tom,
                resultat = KompetanseResultat.valueOf(kompetanse.resultat!!.name),
                sokersaktivitet = SøkersAktivitet.valueOf(kompetanse.søkersAktivitet!!.name)
            )
        }
    }

    @Deprecated("Kan fjernes når vi slutter å publisere på kafka onprem")
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

    private fun hentSøkerV2(behandlingId: Long): PersonDVHV2 {
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId)
        val søker = persongrunnlag.søker
        return lagPersonDVHV2(søker)
    }

    private fun hentSøker(behandlingId: Long): PersonDVH {
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId)
        val søker = persongrunnlag.søker
        return lagPersonDVH(søker)
    }

    private fun hentUtbetalingsperioderV2(behandlingId: Long): List<UtbetalingsperiodeDVHV2> {

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
                mapTilUtbetalingsperiodeV2(
                    segment,
                    andelerForSegment,
                    tilkjentYtelse.behandling,
                    persongrunnlag
                )
            }
    }

    @Deprecated("kan fjernes når vi ikke lenger publiserer hendelser til kafka onprem")
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

    private fun mapTilUtbetalingsperiodeV2(
        segment: LocalDateSegment<Int>,
        andelerForSegment: List<AndelTilkjentYtelse>,
        behandling: Behandling,
        personopplysningGrunnlag: PersonopplysningGrunnlag
    ): UtbetalingsperiodeDVHV2 {
        return UtbetalingsperiodeDVHV2(
            hjemmel = "Ikke implementert",
            stønadFom = segment.fom,
            stønadTom = segment.tom,
            utbetaltPerMnd = segment.value,
            utbetalingsDetaljer = andelerForSegment.filter { it.erAndelSomSkalSendesTilOppdrag() }.map { andel ->
                val personForAndel =
                    personopplysningGrunnlag.søkerOgBarn.find { person -> andel.aktør == person.aktør }
                        ?: throw IllegalStateException("Fant ikke personopplysningsgrunnlag for andel")
                UtbetalingsDetaljDVHV2(
                    person = lagPersonDVHV2(
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

    private fun lagPersonDVHV2(person: Person, delingsProsentYtelse: Int = 0): PersonDVHV2 {
        return PersonDVHV2(
            rolle = person.type.name,
            statsborgerskap = hentStatsborgerskap(person),
            bostedsland = hentLandkode(person),
            delingsprosentYtelse = if (delingsProsentYtelse == 50) delingsProsentYtelse else 0,
            personIdent = person.aktør.aktivFødselsnummer()
        )
    }

    @Deprecated("kan fjernes når vi ikke lenger publiserer hendelser til kafka onprem")
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

    @Deprecated("kan fjernes når vi ikke lenger publiserer hendelser til kafka onprem")
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
