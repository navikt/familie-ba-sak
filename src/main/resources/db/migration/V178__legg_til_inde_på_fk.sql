create INDEX vilkaarsvurdering_fk_idx ON             vilkaarsvurdering(fk_behandling_id);
create INDEX person_resultat_fk_idx ON               person_resultat(fk_vilkaarsvurdering_id);
create INDEX oppgave_fk_idx ON                       oppgave(fk_behandling_id);
create INDEX vilkar_resultat_fk_idx ON               vilkar_resultat(fk_behandling_id);
create INDEX po_statsborgerskap_fk_idx ON            po_statsborgerskap(fk_po_person_id);
create INDEX po_opphold_fk_idx ON                    po_opphold(fk_po_person_id);
create INDEX po_arbeidsforhold_fk_idx ON             po_arbeidsforhold(fk_po_person_id);
create INDEX po_bostedsadresseperiode_fk_idx ON      po_bostedsadresseperiode(fk_po_person_id);
create INDEX behandling_steg_tilstand_fk_idx ON      behandling_steg_tilstand(fk_behandling_id);
create INDEX andel_tilkjent_ytelse_fk_idx ON         andel_tilkjent_ytelse(kilde_behandling_id);
create INDEX annen_vurdering_fk_idx ON               annen_vurdering(fk_person_resultat_id);
create INDEX vilkar_resultat_fk_personr_idx ON     vilkar_resultat(fk_person_resultat_id);
create INDEX tilbakekreving_fk_idx ON                tilbakekreving(fk_behandling_id);
create INDEX okonomi_simulering_mottaker_fk_idx ON   okonomi_simulering_mottaker(fk_behandling_id);
create INDEX po_bostedsadresse_fk_idx ON             po_bostedsadresse(fk_po_person_id);
create INDEX po_sivilstand_fk_idx ON                 po_sivilstand(fk_po_person_id);
create INDEX andel_tilkjent_ytelse_fk_tilkjent_idx ON  andel_tilkjent_ytelse(tilkjent_ytelse_id);
create INDEX endret_utbetaling_andel_fk_idx ON       endret_utbetaling_andel(fk_po_person_id);
create INDEX andel_tilkjent_ytelse_fk_aktoer_idx ON   andel_tilkjent_ytelse(fk_aktoer_id);
create INDEX person_resultat_fk_aktoer_idx ON         person_resultat(fk_aktoer_id);
create INDEX gr_periode_overgangsstonad_fk_idx ON    gr_periode_overgangsstonad(fk_aktoer_id);
create INDEX foedselshendelse_pre_lansering_fk_idx ON foedselshendelse_pre_lansering(fk_aktoer_id);
create INDEX po_person_fk_idx ON                     po_person(fk_aktoer_id);
create INDEX fagsak_fk_idx ON                        fagsak(fk_aktoer_id);