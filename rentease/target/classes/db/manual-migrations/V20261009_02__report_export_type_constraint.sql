-- Add the current Java ReportExportConfig.ReportType values to the existing
-- SQL Server CHECK constraint. This is intentionally a manual migration:
-- Hibernate ddl-auto=update does not reliably replace an existing enum CHECK.
-- No report history rows are changed or deleted.
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @tableId int = OBJECT_ID(N'dbo.report_export_configs', N'U');
    DECLARE @expectedConstraint sysname = N'CK__report_ex__repor__797309D9';
    DECLARE @replacementConstraint sysname = N'CK_report_export_configs_report_type';
    DECLARE @constraintCount int;
    DECLARE @constraintName sysname;
    DECLARE @definition nvarchar(max);
    DECLARE @normalizedDefinition nvarchar(max);
    DECLARE @expectedNormalizedDefinition nvarchar(max) =
        N'REPORT_TYPEISNULLORREPORT_TYPEINN''MONTHLY_REVENUE'',N''VEHICLE_UTILIZATION'',N''MOST_RENTED_VEHICLES'',N''RATING_ANALYSIS''';

    IF @tableId IS NULL
        THROW 51000, N'Expected table dbo.report_export_configs was not found; migration stopped.', 1;

    IF COL_LENGTH(N'dbo.report_export_configs', N'report_type') IS NULL
        THROW 51001, N'Expected column dbo.report_export_configs.report_type was not found; migration stopped.', 1;

    -- Stop rather than silently discarding a value that is present in history
    -- but is not one of the application's supported report types.
    IF EXISTS (
        SELECT 1
        FROM dbo.report_export_configs
        WHERE report_type IS NOT NULL
          AND report_type NOT IN (N'MONTHLY_REVENUE', N'VEHICLE_UTILIZATION',
                                  N'MOST_RENTED_VEHICLES', N'RATING_ANALYSIS')
    )
        THROW 51002, N'Existing report history contains an unsupported report_type. Inspect the rows before migrating.', 1;

    SELECT @constraintCount = COUNT(*)
    FROM sys.check_constraints
    WHERE parent_object_id = @tableId
      AND definition LIKE N'%report_type%';

    IF @constraintCount <> 1
        THROW 51003, N'Expected exactly one CHECK constraint referencing report_type. Inspect sys.check_constraints before migrating.', 1;

    SELECT @constraintName = name, @definition = definition
    FROM sys.check_constraints
    WHERE parent_object_id = @tableId
      AND definition LIKE N'%report_type%';

    -- Return the live definition in the migration output so it is visible in
    -- deployment review and can be compared with the known failing constraint.
    SELECT @constraintName AS current_constraint_name,
           @definition AS current_constraint_definition;

    IF @constraintName = @replacementConstraint
    BEGIN
        SET @normalizedDefinition = UPPER(@definition);
        SET @normalizedDefinition = REPLACE(@normalizedDefinition, N'[', N'');
        SET @normalizedDefinition = REPLACE(@normalizedDefinition, N']', N'');
        SET @normalizedDefinition = REPLACE(@normalizedDefinition, N'(', N'');
        SET @normalizedDefinition = REPLACE(@normalizedDefinition, N')', N'');
        SET @normalizedDefinition = REPLACE(@normalizedDefinition, N' ', N'');
        SET @normalizedDefinition = REPLACE(@normalizedDefinition, CHAR(9), N'');
        SET @normalizedDefinition = REPLACE(@normalizedDefinition, CHAR(10), N'');
        SET @normalizedDefinition = REPLACE(@normalizedDefinition, CHAR(13), N'');

        IF @normalizedDefinition <> @expectedNormalizedDefinition
            THROW 51004, N'The replacement constraint exists but does not match the expected four-value validation. Inspect it manually.', 1;

        COMMIT TRANSACTION;
        PRINT N'Report export type constraint already matches all four supported report types; no changes made.';
    END
    ELSE
    BEGIN
        IF @constraintName <> @expectedConstraint
            THROW 51005, N'The report_type CHECK constraint name differs from the confirmed failing constraint. Inspect it before migrating.', 1;

        IF @definition NOT LIKE N'%report_type%'
            THROW 51006, N'The confirmed constraint does not reference report_type; migration stopped.', 1;

        DECLARE @dropSql nvarchar(max) =
            N'ALTER TABLE dbo.report_export_configs DROP CONSTRAINT ' + QUOTENAME(@constraintName) + N';';
        EXEC sys.sp_executesql @dropSql;

        ALTER TABLE dbo.report_export_configs
            ADD CONSTRAINT CK_report_export_configs_report_type
            CHECK (report_type IS NULL OR report_type IN
                (N'MONTHLY_REVENUE', N'VEHICLE_UTILIZATION', N'MOST_RENTED_VEHICLES', N'RATING_ANALYSIS'));

        SELECT name AS replacement_constraint_name,
               definition AS replacement_constraint_definition
        FROM sys.check_constraints
        WHERE parent_object_id = @tableId
          AND name = @replacementConstraint;

        COMMIT TRANSACTION;
        PRINT N'Report export type constraint now permits the four supported report types and rejects other non-null values.';
    END;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
