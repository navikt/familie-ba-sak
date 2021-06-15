package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.Behandlingutils
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestPutVedtaksperiodeMedBegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakBegrunnelseRepository
import no.nav.familie.ba.sak.behandling.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.behandling.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.behandling.vedtak.domene.tilVedtaksbegrunnelse
import no.nav.familie.ba.sak.behandling.vedtak.domene.tilVedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingRepository
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import org.springframework.stereotype.Service

@Service
class VedtaksperiodeService(
        private val behandlingRepository: BehandlingRepository,
        private val persongrunnlagService: PersongrunnlagService,
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        private val vedtakBegrunnelseRepository: VedtakBegrunnelseRepository,
        private val vedtaksperiodeRepository: VedtaksperiodeRepository,
        private val featureToggleService: FeatureToggleService,
        private val vedtaksperiodeRepository: VedtaksperiodeRepository,
        private val vilkårsvurderingRepository: VilkårsvurderingRepository,
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

            lagre(VedtaksperiodeMedBegrunnelser(
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

                val avslagsperioderMedBegrunnelser = hentAvslagsperioderMedBegrunnelser(vedtak)
                avslagsperioderMedBegrunnelser.forEach { lagre(it) } // TODO funksjon for å lagre flere
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
                mapTilAvslagsperioderDeprecated(
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

        val opphørsperioder = mapTilOpphørsperioder(
                forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
                forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse
        )

        return opphørsperioder
    }

    private fun hentAvslagsperioderMedBegrunnelser(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = vedtak.behandling.id)
                               ?: throw Feil("Fant ikke vilkårsvurdering for behandling ${vedtak.behandling.id} ved generering av avslagsperioder")

        val periodegrupperteAvslagsvilkår: Map<NullablePeriode, List<VilkårResultat>> =
                vilkårsvurdering.personResultater.flatMap { it.vilkårResultater }
                        .filter { it.erEksplisittAvslagPåSøknad == true }
                        .groupBy { NullablePeriode(it.periodeFom, it.periodeTom) }

        return periodegrupperteAvslagsvilkår.map { (fellesPeriode, vilkårResultater) ->

            val begrunnelserMedIdenter: Map<VedtakBegrunnelseSpesifikasjon, List<String>> =
                    begrunnelserMedIdentgrupper(vilkårResultater)

            VedtaksperiodeMedBegrunnelser(vedtak = vedtak,
                                          fom = fellesPeriode.fom,
                                          tom = fellesPeriode.tom,
                                          type = Vedtaksperiodetype.AVSLAG)
                    .apply {
                        begrunnelser.addAll(begrunnelserMedIdenter.map { (begrunnelse, identer) ->
                            Vedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser = this,
                                               vedtakBegrunnelseSpesifikasjon = begrunnelse,
                                               personIdenter = identer
                            )
                        })
                    }
        }
    }

    private fun begrunnelserMedIdentgrupper(vilkårResultater: List<VilkårResultat>): Map<VedtakBegrunnelseSpesifikasjon, List<String>> {
        val begrunnelseOgIdentListe: List<Pair<VedtakBegrunnelseSpesifikasjon, String>> =
                vilkårResultater
                        .map { vilkår ->
                            val personIdent = vilkår.personResultat?.personIdent
                                              ?: throw Feil("VilkårResultat ${vilkår.id} mangler PersonResultat ved sammenslåing av begrunnelser")
                            vilkår.vedtakBegrunnelseSpesifikasjoner.map { begrunnelse -> Pair(begrunnelse, personIdent) }
                        }
                        .flatten()

        return begrunnelseOgIdentListe
                .groupBy { (begrunnelse, ident) -> begrunnelse }
                .mapValues { (begrunnelse, parGruppe) -> parGruppe.map { it.second } }
    }
}