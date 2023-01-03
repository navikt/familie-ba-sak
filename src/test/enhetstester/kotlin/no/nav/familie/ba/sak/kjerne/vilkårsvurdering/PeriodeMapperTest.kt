package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagTidslinjeMedOverlappendePerioder
import no.nav.familie.ba.sak.kjerne.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.fpsak.tidsserie.LocalDateInterval
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PeriodeMapperTest {

    private val datoer = listOf(
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2020, 2, 1),
        LocalDate.of(2020, 3, 1),
        LocalDate.of(2020, 4, 1),
        LocalDate.of(2020, 5, 1),
        LocalDate.of(2020, 6, 1)
    )

    private lateinit var vilkårsvurdering: Vilkårsvurdering

    @BeforeEach
    fun initEach() {
        val fagsak = defaultFagsak()
        val behandling = Behandling(
            fagsak = fagsak,
            kategori = BehandlingKategori.NASJONAL,
            underkategori = BehandlingUnderkategori.ORDINÆR,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            opprettetÅrsak = BehandlingÅrsak.SØKNAD
        ).also {
            it.behandlingStegTilstand.add(BehandlingStegTilstand(0, it, FØRSTE_STEG))
        }

        vilkårsvurdering = lagVilkårsvurdering(randomAktør(), behandling, Resultat.IKKE_VURDERT)
    }

    @Test
    fun `Kombinert tidslinje returnerer rette rette vilkårsresultater for tidsintervaller`() {
        val personResultat =
            PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = randomAktør())

        val tidslinje1 = LocalDateTimeline(
            listOf(
                LocalDateSegment(
                    datoer[0],
                    datoer[2].minusDays(1),
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.UNDER_18_ÅR,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id
                    )
                )
            )
        )
        val tidslinje2 = LocalDateTimeline(
            listOf(
                LocalDateSegment(
                    datoer[1],
                    datoer[3].minusDays(1),
                    VilkårResultat(
                        personResultat = personResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id
                    )
                )
            )
        )

        val kombinertTidslinje = lagTidslinjeMedOverlappendePerioder(listOf(tidslinje1, tidslinje2))

        assert(kombinertTidslinje.toSegments().size == 3)
        val segment1 = kombinertTidslinje.getSegment(LocalDateInterval(datoer[0], datoer[1].minusDays(1)))
        val segment2 = kombinertTidslinje.getSegment(LocalDateInterval(datoer[1], datoer[2].minusDays(1)))
        val segment3 = kombinertTidslinje.getSegment(LocalDateInterval(datoer[2], datoer[3].minusDays(1)))

        assert(segment1.value.size == 1)
        assert(segment2.value.size == 2)
        assert(segment3.value.size == 1)

        assert(segment1.value[0].vilkårType == Vilkår.UNDER_18_ÅR)
        assert(segment2.value[0].vilkårType == Vilkår.UNDER_18_ÅR)
        assert(segment2.value[1].vilkårType == Vilkår.BOSATT_I_RIKET)
        assert(segment3.value[0].vilkårType == Vilkår.BOSATT_I_RIKET)
    }
}
