package no.nav.familie.ba.sak.internal

import no.nav.familie.ba.sak.internal.vedtak.begrunnelser.lagGyldigeBegrunnelserTest
import no.nav.familie.ba.sak.internal.vedtak.vedtaksperioder.lagVedtaksperioderTest
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.KompetanseRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.ValutakursRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TestVerktøyService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vilkårService: VilkårService,
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val endretUtbetalingRepository: EndretUtbetalingAndelRepository,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val vedtakRepository: VedtakRepository,
    private val kompetanseRepository: KompetanseRepository,
    private val utenlandskPeriodebeløpRepository: UtenlandskPeriodebeløpRepository,
    private val valutakursRepository: ValutakursRepository,
    private val søknadGrunnlagService: SøknadGrunnlagService,
) {
    @Transactional
    fun oppdaterVilkårUtenFomTilFødselsdato(behandlingId: Long) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val vilkårsvurdering = vilkårService.hentVilkårsvurdering(behandlingId)

        val persongrunnlag = personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)

        vilkårsvurdering?.personResultater?.forEach { personResultat ->
            personResultat.vilkårResultater.forEach { vilkårResultat ->
                if (vilkårResultat.resultat == Resultat.IKKE_VURDERT) {
                    val person = persongrunnlag?.personer?.find { it.aktør == personResultat.aktør }
                    vilkårResultat.periodeFom =
                        person?.fødselsdato
                    vilkårResultat.resultat = Resultat.OPPFYLT
                    vilkårResultat.begrunnelse = "Opprettet automatisk fra \"Fyll ut vilkårsvurdering\"-knappen"

                    if (behandling.kategori == BehandlingKategori.EØS && vilkårResultat.vilkårType == Vilkår.BOSATT_I_RIKET) {
                        vilkårResultat.utdypendeVilkårsvurderinger = if (person?.type == PersonType.SØKER) listOf(UtdypendeVilkårsvurdering.OMFATTET_AV_NORSK_LOVGIVNING) else listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE)
                    }
                    if (behandling.kategori == BehandlingKategori.EØS && vilkårResultat.vilkårType == Vilkår.BOR_MED_SØKER) {
                        vilkårResultat.utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BARN_BOR_I_NORGE_MED_SØKER)
                    }
                }
            }
        }
    }

    fun hentBegrunnelsetest(behandlingId: Long): String {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)

        val persongrunnlag: PersonopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)!!
        val persongrunnlagForrigeBehandling =
            forrigeBehandling?.let { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it.id)!! }

        val personResultater = vilkårService.hentVilkårsvurderingThrows(behandlingId).personResultater
        val personResultaterForrigeBehandling =
            forrigeBehandling?.let { vilkårService.hentVilkårsvurderingThrows(it.id).personResultater }

        val andeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
        val andelerForrigeBehandling =
            forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) }

        val endredeUtbetalinger = endretUtbetalingRepository.findByBehandlingId(behandlingId)
        val endredeUtbetalingerForrigeBehandling =
            forrigeBehandling?.let { endretUtbetalingRepository.findByBehandlingId(it.id) }

        val kompetanse = kompetanseRepository.finnFraBehandlingId(behandlingId)
        val kompetanseForrigeBehandling =
            forrigeBehandling?.let { kompetanseRepository.finnFraBehandlingId(it.id) }

        val utenlandskePeriodebeløp = utenlandskPeriodebeløpRepository.finnFraBehandlingId(behandlingId)
        val utenlandskePeriodebeløpForrigeBehandling =
            forrigeBehandling?.let { utenlandskPeriodebeløpRepository.finnFraBehandlingId(it.id) }

        val valutakurser = valutakursRepository.finnFraBehandlingId(behandlingId)
        val valutakurserForrigeBehandling =
            forrigeBehandling?.let { valutakursRepository.finnFraBehandlingId(it.id) }

        val vedtaksperioder =
            vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(
                vedtakRepository.findByBehandlingAndAktiv(behandlingId).id,
            )

        return lagGyldigeBegrunnelserTest(
            behandling = behandling,
            forrigeBehandling = forrigeBehandling,
            persongrunnlag = persongrunnlag,
            persongrunnlagForrigeBehandling = persongrunnlagForrigeBehandling,
            personResultater = personResultater,
            personResultaterForrigeBehandling = personResultaterForrigeBehandling,
            andeler = andeler,
            andelerForrigeBehandling = andelerForrigeBehandling,
            vedtaksperioder = vedtaksperioder,
            endredeUtbetalinger = endredeUtbetalinger,
            endredeUtbetalingerForrigeBehandling = endredeUtbetalingerForrigeBehandling,
            kompetanse = kompetanse,
            kompetanseForrigeBehandling = kompetanseForrigeBehandling,
            utenlandskePeriodebeløp = utenlandskePeriodebeløp,
            utenlandskePeriodebeløpForrigeBehandling = utenlandskePeriodebeløpForrigeBehandling,
            valutakurser = valutakurser,
            valutakurserForrigeBehandling = valutakurserForrigeBehandling,
            personerFremstiltKravFor = søknadGrunnlagService.finnPersonerFremstiltKravFor(behandling = behandling, forrigeBehandling = forrigeBehandling),
        )
    }

    fun hentVedtaksperioderTest(behandlingId: Long): String {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        val forrigeBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
        val persongrunnlag: PersonopplysningGrunnlag =
            personopplysningGrunnlagRepository.findByBehandlingAndAktiv(behandlingId)!!
        val persongrunnlagForrigeBehandling =
            forrigeBehandling?.let { personopplysningGrunnlagRepository.findByBehandlingAndAktiv(it.id)!! }
        val personResultater = vilkårService.hentVilkårsvurderingThrows(behandlingId).personResultater
        val personResultaterForrigeBehandling =
            forrigeBehandling?.let { vilkårService.hentVilkårsvurderingThrows(it.id).personResultater }
        val andeler = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
        val andelerForrigeBehandling =
            forrigeBehandling?.let { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(it.id) }
        val endredeUtbetalinger = endretUtbetalingRepository.findByBehandlingId(behandlingId)
        val endredeUtbetalingerForrigeBehandling =
            forrigeBehandling?.let { endretUtbetalingRepository.findByBehandlingId(it.id) }
        val kompetanse = kompetanseRepository.finnFraBehandlingId(behandlingId)
        val kompetanseForrigeBehandling =
            forrigeBehandling?.let { kompetanseRepository.finnFraBehandlingId(it.id) }
        val vedtaksperioder =
            vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(
                vedtakRepository.findByBehandlingAndAktiv(behandlingId).id,
            )

        return lagVedtaksperioderTest(
            behandling = behandling,
            forrigeBehandling = forrigeBehandling,
            persongrunnlag = persongrunnlag,
            persongrunnlagForrigeBehandling = persongrunnlagForrigeBehandling,
            personResultater = personResultater,
            personResultaterForrigeBehandling = personResultaterForrigeBehandling,
            andeler = andeler,
            andelerForrigeBehandling = andelerForrigeBehandling,
            vedtaksperioder = vedtaksperioder,
            endredeUtbetalinger = endredeUtbetalinger,
            endredeUtbetalingerForrigeBehandling = endredeUtbetalingerForrigeBehandling,
            kompetanse = kompetanse,
            kompetanseForrigeBehandling = kompetanseForrigeBehandling,
        )
    }
}
