create table if not exists sweeps (
    id bigserial primary key,
    from_address varchar(128) not null,
    to_address varchar(128) not null,
    amount_wei numeric(38, 0) not null,
    gas_limit numeric(38, 0) not null,
    effective_fee_per_gas_wei numeric(38, 0) not null,
    gas_cost_wei numeric(38, 0) not null,
    nonce numeric(38, 0) not null,
    chain_id numeric(38, 0) not null,
    tx_hash varchar(128),
    status varchar(16) not null,
    created_at timestamp without time zone not null default now(),
    updated_at timestamp without time zone not null default now()
);

create index if not exists idx_sweeps_from_created_at on sweeps (from_address, created_at desc);
