-- Idempotent SQL Server migration for the internal review-resolution workflow.
-- Hibernate's current ddl-auto=update mode can add the nullable columns itself;
-- run this script in environments that apply reviewed/manual schema migrations.
IF COL_LENGTH('dbo.reviews', 'management_status') IS NULL
    ALTER TABLE dbo.reviews ADD management_status varchar(24) NULL;
GO

IF COL_LENGTH('dbo.reviews', 'management_note') IS NULL
    ALTER TABLE dbo.reviews ADD management_note varchar(2000) NULL;
GO

IF COL_LENGTH('dbo.reviews', 'management_updated_at') IS NULL
    ALTER TABLE dbo.reviews ADD management_updated_at datetime2 NULL;
GO

UPDATE dbo.reviews
SET management_status = 'OPEN'
WHERE management_status IS NULL;
GO
