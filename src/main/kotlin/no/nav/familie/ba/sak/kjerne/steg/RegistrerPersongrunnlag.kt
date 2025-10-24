package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.AutovedtakSkalIkkeGjennomføresFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.EøsSkjemaerForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.PersonopplysningGrunnlagForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling.VilkårsvurderingForNyBehandlingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.kombiner
import no.nav.familie.tidslinje.utvidelser.outerJoin
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class RegistrerPersongrunnlag(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val vilkårsvurderingForNyBehandlingService: VilkårsvurderingForNyBehandlingService,
    private val personopplysningGrunnlagForNyBehandlingService: PersonopplysningGrunnlagForNyBehandlingService,
    private val eøsSkjemaerForNyBehandlingService: EøsSkjemaerForNyBehandlingService,
    private val vilkårService: VilkårService,
) : BehandlingSteg<RegistrerPersongrunnlagDTO> {
    @Transactional
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        data: RegistrerPersongrunnlagDTO,
    ): StegType {
        val forrigeBehandlingSomErVedtatt =
            behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(
                behandling,
            )

        personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt,
            søkerIdent = data.ident,
            barnasIdenter = data.barnasIdenter,
        )

        vilkårsvurderingForNyBehandlingService.opprettVilkårsvurderingUtenomHovedflyt(
            behandling = behandling,
            forrigeBehandlingSomErVedtatt = forrigeBehandlingSomErVedtatt,
            nyMigreringsdato = data.nyMigreringsdato,
        )

        eøsSkjemaerForNyBehandlingService.kopierEøsSkjemaer(
            forrigeBehandlingSomErVedtattId =
                if (forrigeBehandlingSomErVedtatt != null) {
                    BehandlingId(
                        forrigeBehandlingSomErVedtatt.id,
                    )
                } else {
                    null
                },
            behandlingId = BehandlingId(behandling.id),
        )

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun postValiderSteg(behandling: Behandling) {
        if (!behandling.erFinnmarksEllerSvalbardtillegg()) {
            return
        }
        val forrigeVedtatteBehandling = behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(behandling)
        if (forrigeVedtatteBehandling == null) {
            throw Feil("Vi kan ikke kjøre behandling med årsak ${behandling.opprettetÅrsak} dersom det ikke finnes en tidligere behandling. Behandling: ${behandling.id}")
        }
        validerAtVilkårsvurderingErEndret(
            vilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(behandling.id),
            forrigeVilkårsvurdering = vilkårService.hentVilkårsvurderingThrows(forrigeVedtatteBehandling.id),
        )
    }

    private fun validerAtVilkårsvurderingErEndret(
        vilkårsvurdering: Vilkårsvurdering,
        forrigeVilkårsvurdering: Vilkårsvurdering,
    ) {
        val bosattIRiketVilkårPerAktør = lagBosattIRiketVilkårTidslinjePerAktør(vilkårsvurdering)
        val forrigeBosattIRiketVilkårPerAktør = lagBosattIRiketVilkårTidslinjePerAktør(forrigeVilkårsvurdering)

        val ingenEndringIBosattIRiketVilkår =
            bosattIRiketVilkårPerAktør
                .outerJoin(forrigeBosattIRiketVilkårPerAktør) { nåværende, forrige ->
                    val erEndringerIUtdypendeVilkårsvurdering = nåværende?.utdypendeVilkårsvurderinger != forrige?.utdypendeVilkårsvurderinger
                    val erEndringerIRegelverk = nåværende?.vurderesEtter != forrige?.vurderesEtter
                    val erVilkårSomErSplittetOpp = nåværende?.periodeFom != forrige?.periodeFom
                    erEndringerIUtdypendeVilkårsvurdering || erEndringerIRegelverk || erVilkårSomErSplittetOpp
                }.values
                .kombiner { erEndringIVilkår -> erEndringIVilkår.any { it } }
                .tilPerioder()
                .all { it.verdi == false }

        if (ingenEndringIBosattIRiketVilkår) {
            throw AutovedtakSkalIkkeGjennomføresFeil("Ingen endring i 'Bosatt i riket'-vilkåret")
        }
    }

    private fun lagBosattIRiketVilkårTidslinjePerAktør(vilkårsvurdering: Vilkårsvurdering) =
        vilkårsvurdering
            .personResultater
            .associate { personResultat ->
                personResultat.aktør.aktørId to
                    personResultat.vilkårResultater
                        .filter { it.vilkårType == BOSATT_I_RIKET }
                        .tilTidslinje()
            }

    override fun stegType(): StegType = StegType.REGISTRERE_PERSONGRUNNLAG
}

data class RegistrerPersongrunnlagDTO(
    val ident: String,
    val barnasIdenter: List<String>,
    val nyMigreringsdato: LocalDate? = null,
)
