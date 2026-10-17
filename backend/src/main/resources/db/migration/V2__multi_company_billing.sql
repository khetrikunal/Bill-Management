alter table company_settings
  add column display_name varchar(200) not null default '',
  add column billing_address text not null default '',
  add column pan varchar(10) not null default '',
  add column upi_id varchar(120) not null default '',
  add column default_payment_terms varchar(500) not null default '',
  add column authorized_signatory varchar(200) not null default '',
  add column signatory_designation varchar(120) not null default '';

update company_settings
set display_name = 'SSET',
    billing_address = address
where id = 1;

insert into company_settings (id, company_name, display_name, address, billing_address, phone, email, gstin,
  pan, bank_name, bank_account, bank_branch, ifsc, upi_id, declaration, signature_text, footer_note, logo,
  default_currency, default_cgst_rate, default_sgst_rate, page_size, invoice_prefix, number_padding,
  include_financial_year, default_payment_terms, authorized_signatory, signatory_designation)
values (2, 'PK Corporate Fabric Manufacturing', 'PK Corporate Fabric Manufacturing', '', '', '', '', '',
  '', '',   '', '', '', '', '', '', '', null, 'INR', 0, 0, 'A4', 'PKCFM', 4, true, '', '', '');

create table app_user_company (
  id bigserial primary key,
  user_id bigint not null references app_user(id) on delete cascade,
  company_id bigint not null references company_settings(id) on delete cascade,
  constraint ux_app_user_company unique (user_id, company_id)
);
insert into app_user_company (user_id, company_id)
select u.id, c.id from app_user u cross join company_settings c;

alter table bill add column company_id bigint not null default 1 references company_settings(id);
alter table bill add column due_date date;
alter table bill add column payment_terms varchar(500) not null default '';
alter table bill add column buyer_phone varchar(60) not null default '';
alter table bill add column buyer_email varchar(200) not null default '';
alter table bill add column shipping_address text not null default '';
alter table customer add column company_id bigint not null default 1 references company_settings(id);
alter table product add column company_id bigint not null default 1 references company_settings(id);
create index ix_customer_company_name on customer(company_id, lower(name));
create index ix_product_company_name on product(company_id, lower(name));
update number_sequence set series_key = '1|' || series_key;
alter table bill drop constraint ux_bill_invoice_number;
create unique index ux_bill_company_invoice_number on bill(company_id, invoice_number) where invoice_number is not null;
create index ix_bill_company_date on bill(company_id, invoice_date);

create table bill_payment (
  id bigserial primary key,
  bill_id bigint not null references bill(id),
  amount numeric(16,2) not null check (amount > 0),
  payment_date date not null,
  method varchar(30) not null,
  transaction_reference varchar(120) not null default '',
  notes text not null default '',
  recorded_by varchar(60) not null,
  created_at timestamptz not null default now()
);
create index ix_bill_payment_bill on bill_payment(bill_id, payment_date, id);
