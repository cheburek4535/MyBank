create table if not exists accounts (
    id varchar(255) primary key,
    owner text not null,
    balance bigint not null default 0 check (balance >= 0),
    status varchar(20) not null default 'OPEN' check (status in ('OPEN', 'CLOSED', 'FROZEN'))
    created_at timestamptz not null default now()
);
create table if not exists transactions (
    id varchar(255) primary key,
    type varchar(20) not null check (type in ('TRANSFER', 'DEPOSIT', 'WITHDRAW')),
    idempotency_key varchar(255) not null unique,
    amount bigint not null check ( amount > 0 ),
    sender_account_id varchar(255) references accounts(id),
    recipient_account_id varchar(255) references accounts(id),
    status varchar(20) not null default 'SUCCESS' check (status in ('SUCCESS', 'FAILED')),
    created_at timestamptz not null default now(),
    constraint check_transaction_accounts CHECK (
        (type = 'TRANSFER' and sender_account_id is not null and recipient_account_id is not null and sender_account_id <> recipient_account_id)
        or (type = 'DEPOSIT' and sender_account_id is null and recipient_account_id is not null)
        or (type = 'WITHDRAW' and sender_account_id is not null and recipient_account_id is null)
        )
);

create index if not exists idx_tx_sender_account on transactions (sender_account_id);
create index if not exists idx_tx_recipient_account on transactions (recipient_account_id);