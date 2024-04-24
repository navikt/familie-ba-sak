CREATE TABLE IF NOT EXISTS vurderingsstrategi_for_valutakurser
(
    id                                  BIGINT PRIMARY KEY,
    fk_behandling_id                    BIGINT REFERENCES behandling (id) ON DELETE CASCADE NOT NULL,
    vurderingsstrategi_for_valutakurser TEXT                                                NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS vurderingsstrategi_for_valutakurser_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE UNIQUE INDEX IF NOT EXISTS vurderingsstrategi_for_valutakurser_fk_behandling_id_idx ON vurderingsstrategi_for_valutakurser (fk_behandling_id);