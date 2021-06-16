package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode


import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedBegrunnelse
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.kjerne.dokument.hentVedtaksbrevtype
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelseRepository
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilVedtaksbegrunnelseFritekst
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class VedtaksperiodeService(
        private val behandlingRepository: BehandlingRepository,
        private val persongrunnlagService: PersongrunnlagService,
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        private val vedtakBegrunnelseRepository: VedtakBegrunnelseRepository,
        private val vedtaksperiodeRepository: VedtaksperiodeRepository,
        private val featureToggleService: FeatureToggleService,
) {

    fun lagre(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): VedtaksperiodeMedBegrunnelser {
        return vedtaksperiodeRepository.save(vedtaksperiodeMedBegrunnelser)
    }

    fun slettVedtaksperioderFor(vedtak: Vedtak) {
        vedtaksperiodeRepository.slettVedtaksperioderFor(vedtak)
    }

    fun oppdaterVedtaksperiodeMedBegrunnelser(vedtaksperiodeId: Long,
                                              restPutVedtaksperiodeMedBegrunnelse: RestPutVedtaksperiodeMedBegrunnelse): Vedtak {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)

        vedtaksperiodeMedBegrunnelser.settBegrunnelser(restPutVedtaksperiodeMedBegrunnelse.begrunnelser.map {
            it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser)
        })

        vedtaksperiodeMedBegrunnelser.settFritekster(restPutVedtaksperiodeMedBegrunnelse.fritekster.map {
            tilVedtaksbegrunnelseFritekst(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser, fritekst = it)
        })

        lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun oppdaterVedtakMedVedtaksperioder(vedtak: Vedtak) {

        slettVedtaksperioderFor(vedtak)
        if (vedtak.behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET) {

            val vedtakstype = hentVedtaksbrevtype(vedtak.behandling)
            val erAutobrevFor6Og18År = vedtakstype == Vedtaksbrevtype.AUTOVEDTAK_BARN6_ÅR
                                       || vedtakstype == Vedtaksbrevtype.AUTOVEDTAK_BARN18_ÅR

            val fom = if (erAutobrevFor6Og18År) {
                YearMonth.now().førsteDagIInneværendeMåned()
            } else null

            val tom = if (erAutobrevFor6Og18År) {
                finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(vedtak.behandling.id)
            } else null

            lagre(VedtaksperiodeMedBegrunnelser(
                    fom = fom,
                    tom = tom,
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.FORTSATT_INNVILGET
            ))
        } else {
            if (featureToggleService.isEnabled(FeatureToggleConfig.BRUK_VEDTAKSTYPE_MED_BEGRUNNELSER)) {
                val vedtaksperioder = hentUtbetalingsperioder(vedtak.behandling) + hentOpphørsperioder(vedtak.behandling)
                vedtaksperioder.forEach {
                    lagre(VedtaksperiodeMedBegrunnelser(
                            fom = it.periodeFom,
                            tom = it.periodeTom,
                            vedtak = vedtak,
                            type = it.vedtaksperiodetype
                    ))
                }
            }
        }
    }

    fun kopierOverVedtaksperioder(deaktivertVedtak: Vedtak, aktivtVedtak: Vedtak) {
        val gamleVedtaksperioder = vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId = deaktivertVedtak.id)

        gamleVedtaksperioder.forEach { vedtaksperiodeMedBegrunnelser ->
            lagre(VedtaksperiodeMedBegrunnelser(
                    vedtak = aktivtVedtak,
                    fom = vedtaksperiodeMedBegrunnelser.fom,
                    tom = vedtaksperiodeMedBegrunnelser.tom,
                    type = vedtaksperiodeMedBegrunnelser.type,
                    begrunnelser = vedtaksperiodeMedBegrunnelser.begrunnelser.map { it.kopier(vedtaksperiodeMedBegrunnelser) }
                            .toMutableSet(),
                    fritekster = vedtaksperiodeMedBegrunnelser.fritekster.map { it.kopier(vedtaksperiodeMedBegrunnelser) }
                            .toMutableSet()
            ))
        }
    }

    fun hentPersisterteVedtaksperioder(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        return vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId = vedtak.id)
    }

    fun oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(vedtak: Vedtak,
                                                               vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon) {
        val vedtaksperioder = hentPersisterteVedtaksperioder(vedtak)

        val fortsattInnvilgetPeriode: VedtaksperiodeMedBegrunnelser =
                vedtaksperioder.singleOrNull() ?: throw Feil("Finner ingen eller flere vedtaksperioder ved fortsatt innvilget")

        fortsattInnvilgetPeriode.settBegrunnelser(listOf(
                Vedtaksbegrunnelse(
                        vedtaksperiodeMedBegrunnelser = fortsattInnvilgetPeriode,
                        vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
                        personIdenter = hentPersonIdenterFraUtbetalingsperiode(hentUtbetalingsperioder(vedtak.behandling))
                )
        ))

        lagre(fortsattInnvilgetPeriode)
    }

    private fun finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(behandlingId: Long): LocalDate =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandlingId))
                    .filter { it.stønadFom <= YearMonth.now() && it.stønadTom >= YearMonth.now() }
                    .minByOrNull { it.stønadTom }?.stønadTom?.sisteDagIInneværendeMåned()

            ?: error("Fant ikke andel for tilkjent ytelse inneværende måned for behandling $behandlingId.")

    fun hentUtbetalingsperioder(behandling: Behandling): List<Utbetalingsperiode> {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                                       ?: return emptyList()
        val andelerTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        return mapTilUtbetalingsperioder(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag
        )
    }

    fun hentVedtaksperioder(behandling: Behandling): List<Vedtaksperiode> {
        if (behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET) return emptyList()

        val iverksatteBehandlinger =
                behandlingRepository.finnIverksatteBehandlinger(fagsakId = behandling.fagsak.id)

        val forrigeIverksatteBehandling: Behandling? = Behandlingutils.hentForrigeIverksatteBehandling(
                iverksatteBehandlinger = iverksatteBehandlinger,
                behandlingFørFølgende = behandling
        )

        val forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? =
                if (forrigeIverksatteBehandling != null)
                    persongrunnlagService.hentAktiv(behandlingId = forrigeIverksatteBehandling.id) else null
        val forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse> =
                if (forrigeIverksatteBehandling != null)
                    andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                            behandlingId = forrigeIverksatteBehandling.id
                    ) else emptyList()

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                                       ?: return emptyList()
        val andelerTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        val utbetalingsperioder = hentUtbetalingsperioder(behandling)

        val opphørsperioder = mapTilOpphørsperioder(
                forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
                forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse
        )

        val avslagsperioder =
                mapTilAvslagsperioder(
                        vedtakBegrunnelser = vedtakBegrunnelseRepository.finnForBehandling(behandlingId = behandling.id)
                )

        return utbetalingsperioder + opphørsperioder + avslagsperioder
    }

    fun hentOpphørsperioder(behandling: Behandling): List<Opphørsperiode> {
        if (behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET) return emptyList()

        val iverksatteBehandlinger =
                behandlingRepository.finnIverksatteBehandlinger(fagsakId = behandling.fagsak.id)

        val forrigeIverksatteBehandling: Behandling? = Behandlingutils.hentForrigeIverksatteBehandling(
                iverksatteBehandlinger = iverksatteBehandlinger,
                behandlingFørFølgende = behandling
        )

        val forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? =
                if (forrigeIverksatteBehandling != null)
                    persongrunnlagService.hentAktiv(behandlingId = forrigeIverksatteBehandling.id)
                else null
        val forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse> =
                if (forrigeIverksatteBehandling != null) andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                        behandlingId = forrigeIverksatteBehandling.id) else emptyList()

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                                       ?: return emptyList()
        val andelerTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        return mapTilOpphørsperioder(
                forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
                forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse
        )
    }
}