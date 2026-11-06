# RentEase - SQL Server Express setup

This version is configured for the SQL Server instance shown in SSMS:

    .\\SQLEXPRESS

and uses Windows Authentication.

## 1. Create the database

In SSMS, connect to `.\\SQLEXPRESS` and run:

```sql
IF DB_ID('RentEaseDB') IS NULL
    CREATE DATABASE RentEaseDB;
GO
```

## 2. Make sure SQL Server Express is running

Open `services.msc` and make sure **SQL Server (SQLEXPRESS)** is running.

Because the JDBC URL uses `instanceName=SQLEXPRESS`, the SQL Server Browser service should also be running so the driver can resolve the named instance's TCP port.

## 3. Enable TCP/IP if required

Open SQL Server Configuration Manager:

SQL Server Network Configuration -> Protocols for SQLEXPRESS -> TCP/IP

Set TCP/IP to **Enabled**. Restart **SQL Server (SQLEXPRESS)** after changing it.

## 4. Windows Authentication JDBC library

The Microsoft JDBC Driver uses a native authentication DLL for Windows integrated authentication. The project is configured with:

`integratedSecurity=true;authenticationScheme=NativeAuthentication`

Microsoft documents that this mode loads `mssql-jdbc_auth-<version>-<arch>.dll` on Windows.

For the JDBC driver version used by this project (12.6.4), obtain the **12.6.4 x64** authentication DLL and place it here:

`native\\mssql-jdbc_auth-12.6.4.x64.dll`

If Maven is available, you can download the Microsoft authentication artifact with:

```powershell
mvn dependency:get -Dartifact=com.microsoft.sqlserver:mssql-jdbc_auth:12.6.4.x64:dll
```

Then copy the downloaded `mssql-jdbc_auth-12.6.4.x64.dll` from your local Maven repository into the project's `native` folder.

## 5. IntelliJ IDEA

A run configuration named **RentEase - SQL Server Express** is included.

Select it and Run. It already sets:

```text
-Djava.library.path=$PROJECT_DIR$/native
```

If the configuration does not appear immediately, reload/reopen the Maven project and select it from the Run Configuration dropdown.

## 6. Open the website

After the console shows that Tomcat has started and the application has started successfully:

`http://localhost:8080`

## If you get an error

- `Connection refused` -> SQL Server/SQLEXPRESS or SQL Server Browser is not reachable.
- `This driver is not configured for integrated authentication` / native authentication DLL error -> check the DLL in `native` and the IntelliJ VM option.
- `Cannot open database RentEaseDB` -> create `RentEaseDB` in SSMS.
