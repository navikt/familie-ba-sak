-- NAV-29456: Inaktive vilkårsvurderinger skal slettes i stedet for å bli liggende.
-- ON DELETE CASCADE gjør at sletting av en vilkårsvurdering også sletter tilhørende
-- person_resultat, vilkar_resultat og annen_vurdering.
-- Constraintene legges til med NOT VALID for å unngå full skann av store tabeller mens
-- ACCESS EXCLUSIVE-låsen holdes. De valideres ikke her: de erstatter allerede gyldige
-- FK-er på de samme kolonnene, så ingen rader kan bryte dem. Eventuell VALIDATE CONSTRAINT
-- kan kjøres etter opprydding av gamle inaktive rader, når tabellene er mindre.

ALTER TABLE person_resultat
    DROP CONSTRAINT periode_resultat_fk_behandling_resultat_id_fkey, -- Navn fra før tabellene ble omdøpt i V43 og V103
    ADD CONSTRAINT person_resultat_fk_vilkaarsvurdering_id_fkey
        FOREIGN KEY (fk_vilkaarsvurdering_id)
            REFERENCES vilkaarsvurdering (id)
            ON DELETE CASCADE
            NOT VALID;

ALTER TABLE vilkar_resultat
    DROP CONSTRAINT vilkar_resultat_fk_person_resultat_id_fkey,
    ADD CONSTRAINT vilkar_resultat_fk_person_resultat_id_fkey
        FOREIGN KEY (fk_person_resultat_id)
            REFERENCES person_resultat (id)
            ON DELETE CASCADE
            NOT VALID;

ALTER TABLE annen_vurdering
    DROP CONSTRAINT annen_vurdering_fk_person_resultat_id_fkey,
    ADD CONSTRAINT annen_vurdering_fk_person_resultat_id_fkey
        FOREIGN KEY (fk_person_resultat_id)
            REFERENCES person_resultat (id)
            ON DELETE CASCADE
            NOT VALID;
