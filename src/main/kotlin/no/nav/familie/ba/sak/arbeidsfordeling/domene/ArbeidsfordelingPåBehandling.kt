package no.nav.familie.ba.sak.arbeidsfordeling.domene

import javax.persistence.*

@Entity(name = "ArbeidsfordelingPåBehandling")
@Table(name = "ARBEIDSFORDELING_PA_BEHANDLING")
data class ArbeidsfordelingPåBehandling(
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "arbeidsfordeling_pa_behandling_seq_generator")
        @SequenceGenerator(name = "arbeidsfordeling_pa_behandling_seq_generator",
                           sequenceName = "arbeidsfordeling_pa_behandling_seq",
                           allocationSize = 50)
        val id: Long = 0,

        @Column(name = "fk_behandling_id", nullable = false, updatable = false, unique = true)
        val behandlingId: Long,

        @Column(name = "behandlende_enhet_id", nullable = false)
        var behandlendeEnhetId: String,

        @Column(name = "behandlende_enhet_navn", nullable = false)
        var behandlendeEnhetNavn: String,

        @Column(name = "manuelt_overstyrt", nullable = false)
        var manueltOverstyrt: Boolean = false,
)

