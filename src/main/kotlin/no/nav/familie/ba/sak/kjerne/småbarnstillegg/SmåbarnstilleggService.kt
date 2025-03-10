package no.nav.familie.ba.sak.kjerne.småbarnstillegg

import no.nav.familie.ba.sak.common.MånedPeriode
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import org.springframework.stereotype.Service

@Service
class SmåbarnstilleggService(
    private val beregningService: BeregningService,
) {
    fun kanAutomatiskIverksetteSmåbarnstilleggEndring(
        behandling: Behandling,
        sistIverksatteBehandling: Behandling?,
    ): Boolean {
        if (!behandling.skalBehandlesAutomatisk || !behandling.erSmåbarnstillegg()) return false

        val (innvilgedeMånedPerioder, reduserteMånedPerioder) =
            finnInnvilgedeOgReduserteAndelerSmåbarnstillegg(
                behandling = behandling,
                sistIverksatteBehandling = sistIverksatteBehandling,
            )

        return kanAutomatiskIverksetteSmåbarnstillegg(
            innvilgedeMånedPerioder = innvilgedeMånedPerioder,
            reduserteMånedPerioder = reduserteMånedPerioder,
        )
    }

    fun finnInnvilgedeOgReduserteAndelerSmåbarnstillegg(
        sistIverksatteBehandling: Behandling?,
        behandling: Behandling,
    ): Pair<List<MånedPeriode>, List<MånedPeriode>> {
        val forrigeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) {
                emptyList()
            } else {
                beregningService
                    .hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                        behandlingId = sistIverksatteBehandling.id,
                    ).filter { it.erSmåbarnstillegg() }
            }

        val nyeSmåbarnstilleggAndeler =
            if (sistIverksatteBehandling == null) {
                emptyList()
            } else {
                beregningService
                    .hentAndelerTilkjentYtelseMedUtbetalingerForBehandling(
                        behandlingId = behandling.id,
                    ).filter { it.erSmåbarnstillegg() }
            }

        return hentInnvilgedeOgReduserteAndelerSmåbarnstillegg(
            forrigeSmåbarnstilleggAndeler = forrigeSmåbarnstilleggAndeler,
            nyeSmåbarnstilleggAndeler = nyeSmåbarnstilleggAndeler,
        )
    }
}
