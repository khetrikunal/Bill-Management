create table app_user (
  id bigserial primary key,
  username varchar(60) not null,
  password_hash varchar(100) not null,
  full_name varchar(120) not null,
  role varchar(20) not null,
  active boolean not null default true,
  created_at timestamptz not null default now()
);
create unique index ux_app_user_username on app_user (lower(username));

create table company_settings (
  id bigint primary key,
  company_name varchar(200) not null,
  address text not null default '',
  phone varchar(200) not null default '',
  email varchar(200) not null default '',
  gstin varchar(20) not null default '',
  bank_name varchar(120) not null default '',
  bank_account varchar(60) not null default '',
  bank_branch varchar(120) not null default '',
  ifsc varchar(20) not null default '',
  declaration text not null default '',
  signature_text varchar(300) not null default '',
  footer_note text not null default '',
  logo text,
  default_currency varchar(3) not null default 'INR',
  default_cgst_rate numeric(5,2) not null default 9,
  default_sgst_rate numeric(5,2) not null default 9,
  page_size varchar(10) not null default 'A4',
  invoice_prefix varchar(20) not null default 'INV',
  number_padding int not null default 4,
  include_financial_year boolean not null default true
);

-- Business details taken from the supplied Excel file (Sheet1). Editable in Company Settings.
insert into company_settings (id, company_name, address, phone, email, gstin, bank_name, bank_account,
  bank_branch, ifsc, declaration, signature_text, invoice_prefix)
values (1, 'Shree Siddhnath Enterprises & Transport',
  E'Shop no 11, Patil Plaza,\nNear Jalochi Rd, Suryanagari,\nBaramati-413133',
  '+91 8999835604, +91 8446443112', 'shreesiddhnath4242@gmail.com', '27CZOPK6914Q1ZE',
  'Bank of Maharashtra', '60353367573', 'Mundhwa, Pune', 'MAHB0000135',
  'We declare that this invoice shows the actual price of the Goods described and that all particulars are true and correct',
  'SHREE SIDDHNATH ENTERPRISES & TRANSPORT', 'SSE');

create table customer (
  id bigserial primary key,
  name varchar(200) not null,
  address text not null default '',
  gstin varchar(20) not null default '',
  email varchar(200) not null default '',
  phone varchar(60) not null default ''
);
create index ix_customer_name on customer (lower(name));

create table product (
  id bigserial primary key,
  name varchar(300) not null,
  item_code varchar(80) not null default '',
  hsn varchar(12) not null default '',
  unit varchar(20) not null default 'nos',
  rate numeric(14,2) not null default 0,
  cgst_rate numeric(5,2) not null default 9,
  sgst_rate numeric(5,2) not null default 9,
  active boolean not null default true
);
create index ix_product_name on product (lower(name));

create table bill (
  id bigserial primary key,
  doc_type varchar(30) not null,
  status varchar(12) not null,
  invoice_number varchar(40),
  invoice_date date not null,
  transaction_ref varchar(60),
  po_number varchar(60),
  buyer_id bigint,
  buyer_name varchar(200) not null,
  buyer_address text not null default '',
  buyer_gstin varchar(20) not null default '',
  currency varchar(3) not null default 'INR',
  seller_snapshot text,
  notes text,
  total_taxable numeric(16,2) not null default 0,
  total_cgst numeric(16,2) not null default 0,
  total_sgst numeric(16,2) not null default 0,
  total_tax numeric(16,2) not null default 0,
  grand_total numeric(16,2) not null default 0,
  amount_in_words varchar(500) not null default '',
  created_by varchar(60) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  finalized_at timestamptz,
  cancelled_at timestamptz,
  cancel_reason text,
  duplicated_from bigint,
  version bigint not null default 0,
  constraint ux_bill_invoice_number unique (invoice_number)
);
create index ix_bill_date on bill (invoice_date);
create index ix_bill_status on bill (status);
create index ix_bill_buyer on bill (lower(buyer_name));
create index ix_bill_txn on bill (transaction_ref);

create table bill_item (
  id bigserial primary key,
  bill_id bigint not null references bill(id) on delete cascade,
  line_no int not null,
  po_number varchar(60),
  name varchar(300) not null,
  item_code varchar(80),
  hsn varchar(12),
  qty numeric(14,3) not null,
  unit varchar(20) not null,
  rate numeric(14,2) not null,
  cgst_rate numeric(5,2) not null,
  sgst_rate numeric(5,2) not null,
  taxable numeric(16,2) not null,
  cgst numeric(16,2) not null,
  sgst numeric(16,2) not null,
  total numeric(16,2) not null
);
create index ix_bill_item_bill on bill_item (bill_id);

create table bill_audit (
  id bigserial primary key,
  bill_id bigint not null,
  action varchar(40) not null,
  username varchar(60) not null,
  detail text,
  created_at timestamptz not null default now()
);
create index ix_bill_audit_bill on bill_audit (bill_id);

create table number_sequence (
  id bigserial primary key,
  series_key varchar(80) not null unique,
  last_value bigint not null default 0
);
