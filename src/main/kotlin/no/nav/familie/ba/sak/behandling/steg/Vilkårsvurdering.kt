package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.SakType.Companion.hentSakType
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils
import no.nav.familie.ba.sak.common.toPeriode
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.nare.core.evaluations.Resultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Vilkårsvurdering(
        private val søknadGrunnlagService: SøknadGrunnlagService,
        private val vilkårService: VilkårService,
        private val vedtakService: VedtakService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService,
        private val behandlingResultatService: BehandlingResultatService,
        private val behandlingService: BehandlingService
) : BehandlingSteg<String> {

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: String): StegType {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandling.id)
                                       ?: error("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(behandlingId = behandling.id)
                                 ?: throw Feil("Fant ikke aktiv behandlingresultat på behandling ${behandling.id}")

        if (behandling.opprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE) {
            vilkårService.vurderVilkårForFødselshendelse(behandling.id)
        }

        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                behandling,
                personopplysningGrunnlag)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        behandlingResultatService.loggOpprettBehandlingsresultat(behandlingResultat, behandling)

        if (behandling.opprinnelse == BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE) {
            behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.GODKJENT)
        }

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }

    override fun postValiderSteg(behandling: Behandling) {
        if (behandling.type != BehandlingType.TEKNISK_OPPHØR && behandling.type != BehandlingType.MIGRERING_FRA_INFOTRYGD_OPPHØRT) {
            val behandlingResultat = vilkårService.hentVilkårsvurdering(behandlingId = behandling.id)
                                     ?: error("Finner ikke vilkårsvurdering på behandling ved validering.")

            val listeAvFeil = mutableListOf<String>()

            val periodeResultater = behandlingResultat.periodeResultater(brukMåned = false)

            val søknadDTO = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)?.hentSøknadDto()
            val sakType = hentSakType(behandlingKategori = behandling.kategori, søknadDTO = søknadDTO)


            val harGyldigePerioder = periodeResultater.any { periodeResultat ->
                periodeResultat.allePåkrevdeVilkårVurdert(PersonType.SØKER,
                                                          sakType) &&
                periodeResultat.allePåkrevdeVilkårVurdert(PersonType.BARN,
                                                          sakType)
            }

            when {
                !harGyldigePerioder -> {
                    listeAvFeil.add("Vurderingen har ingen perioder hvor alle påkrevde vilkår er vurdert.")
                }
            }

            val barna = persongrunnlagService.hentBarna(behandling)
            barna.map { barn ->
                behandlingResultat.personResultater
                        .flatMap { it.vilkårResultater }
                        .filter { it.personResultat?.personIdent == barn.personIdent.ident }
                        .forEach { vilkårResultat ->
                            if (vilkårResultat.resultat == Resultat.JA && vilkårResultat.periodeFom == null) {
                                listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato} mangler fom dato.")
                            }
                            if (vilkårResultat.periodeFom != null && vilkårResultat.toPeriode().fom.isBefore(barn.fødselsdato)) {
                                listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato} har fra-og-med dato før barnets fødselsdato.")
                            }
                            if (vilkårResultat.periodeFom != null &&
                                vilkårResultat.toPeriode().fom.isAfter(barn.fødselsdato.plusYears(18))) {
                                listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato} har fra-og-med dato etter barnet har fylt 18.")
                            }
                        }
            }

            if (listeAvFeil.isNotEmpty()) {
                throw Feil(message = "Validering av vilkårsvurdering feilet for behandling ${behandling.id}",
                           frontendFeilmelding = RessursUtils.lagFrontendMelding("Vilkårsvurderingen er ugyldig med følgende feil:",
                                                                                 listeAvFeil)
                )
            }
        }
    }
}