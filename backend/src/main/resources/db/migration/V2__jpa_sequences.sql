-- B6.B: Hibernate cấp ID theo khối 50 (@SequenceGenerator allocationSize = 50) để insert theo batch.
-- Chỉ đổi bước nhảy của sequence cho các bảng ghi nhiều; cấu trúc bảng không đổi.
ALTER SEQUENCE customer.customers_customer_id_seq INCREMENT BY 50;
ALTER SEQUENCE customer.consents_consent_id_seq INCREMENT BY 50;
ALTER SEQUENCE catalog.product_prices_price_id_seq INCREMENT BY 50;
ALTER SEQUENCE pos.shifts_shift_id_seq INCREMENT BY 50;
ALTER SEQUENCE pos.sales_transactions_transaction_id_seq INCREMENT BY 50;
ALTER SEQUENCE pos.transaction_lines_line_id_seq INCREMENT BY 50;
ALTER SEQUENCE pos.payments_payment_id_seq INCREMENT BY 50;
ALTER SEQUENCE loyalty.point_redemptions_redemption_id_seq INCREMENT BY 50;
ALTER SEQUENCE loyalty.point_lots_lot_id_seq INCREMENT BY 50;
ALTER SEQUENCE loyalty.point_ledgers_ledger_id_seq INCREMENT BY 50;
ALTER SEQUENCE loyalty.tier_histories_history_id_seq INCREMENT BY 50;
ALTER SEQUENCE migration.import_errors_error_id_seq INCREMENT BY 50;
ALTER SEQUENCE migration.customer_source_records_source_id_seq INCREMENT BY 50;
ALTER SEQUENCE shared.outbox_messages_outbox_id_seq INCREMENT BY 50;
ALTER SEQUENCE shared.audit_logs_audit_id_seq INCREMENT BY 50;
