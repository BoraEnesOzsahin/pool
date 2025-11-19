create table if not exists nonce_locks (
    address varchar(64) not null primary key,
    last_nonce numeric(38, 0) not null,
    updated_at timestamp without time zone not null default now()
);
