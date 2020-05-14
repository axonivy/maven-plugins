-- ---------------------------------------------------------------------------------
-- 
-- This script was automatically generated. Do not edit it. Instead edit the source file
-- 
-- ---------------------------------------------------------------------------------
-- Database: Microsoft SQL Server
-- ---------------------------------------------------------------------------------
-- Copyright:
-- AXON IVY AG, Baarerstrasse 12, 6300 Zug
-- ---------------------------------------------------------------------------------

COMMIT
GO

SET IMPLICIT_TRANSACTIONS OFF
GO

-- 
-- Alter database so that read operations are not blocked by write operations.
-- 
ALTER DATABASE [${databaseName}] SET ALLOW_SNAPSHOT_ISOLATION ON
GO

ALTER DATABASE [${databaseName}] SET READ_COMMITTED_SNAPSHOT ON
GO

-- 
-- Alter database so that recursive triggers work.
-- 
ALTER DATABASE [${databaseName}] SET RECURSIVE_TRIGGERS ON
GO


SET IMPLICIT_TRANSACTIONS ON
GO

CREATE TABLE IWA_Task
(
  TaskId BIGINT NOT NULL,
  ActivatorRoleId BIGINT NULL,
  ActivatorUserId BIGINT NULL,
  ExpiryActivatorRoleId BIGINT NULL,
  ExpiryActivatorUserId BIGINT NULL,
  IsExpired BIT NOT NULL,
  PRIMARY KEY (TaskId)
)
GO

CREATE TABLE IWA_Case
(
  CaseId BIGINT NOT NULL
)
GO

CREATE TABLE IWA_User
(
  UserId BIGINT NOT NULL,
  Name VARCHAR(200) NOT NULL,
  UserState INTEGER NOT NULL DEFAULT 0,
  FullName VARCHAR(200) NULL DEFAULT ''
)
GO

CREATE TABLE IWA_Role
(
  RoleId BIGINT NOT NULL,
  Name VARCHAR(200) NOT NULL,
  DisplayNameTemplate VARCHAR(200) NULL
)
GO

ALTER TABLE IWA_Task ADD FOREIGN KEY (ActivatorRoleId) REFERENCES IWA_Role(RoleId) ON DELETE SET NULL
GO

ALTER TABLE IWA_Task ADD FOREIGN KEY (ActivatorUserId) REFERENCES IWA_User(UserId) ON DELETE SET NULL
GO

ALTER TABLE IWA_Task ADD FOREIGN KEY (ExpiryActivatorRoleId) REFERENCES IWA_Role(RoleId) ON DELETE SET NULL
GO

ALTER TABLE IWA_Task ADD FOREIGN KEY (ExpiryActivatorUserId) REFERENCES IWA_User(UserId) ON DELETE SET NULL
GO

CREATE VIEW IWA_TaskQuery
(
  TaskId,
  ActivatorDisplayName,
  ExpiryActivatorDisplayName,
  CurrentActivatorDisplayName,
  IsUnassigned
)
AS
  SELECT
    IWA_Task.TaskId,
    CASE WHEN IWA_Task.ActivatorUserId IS NOT NULL AND LEN(ActivatorUser.FullName) > 0 THEN ActivatorUser.FullName WHEN IWA_Task.ActivatorUserId IS NOT NULL AND LEN(ActivatorUser.FullName) = 0 THEN ActivatorUser.Name WHEN IWA_Task.ActivatorRoleId IS NOT NULL AND LEN(ActivatorRole.DisplayNameTemplate) > 0 THEN ActivatorRole.DisplayNameTemplate WHEN IWA_Task.ActivatorRoleId IS NOT NULL AND LEN(ActivatorRole.DisplayNameTemplate) = 0 THEN ActivatorRole.Name END,
    CASE WHEN IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LEN(ExpiryActivatorUser.FullName) > 0 THEN ExpiryActivatorUser.FullName WHEN IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LEN(ExpiryActivatorUser.FullName) = 0 THEN ExpiryActivatorUser.Name WHEN IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LEN(ExpiryActivatorRole.DisplayNameTemplate) > 0 THEN ExpiryActivatorRole.DisplayNameTemplate WHEN IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LEN(ExpiryActivatorRole.DisplayNameTemplate) = 0 THEN ExpiryActivatorRole.Name ELSE NULL END,
    CASE WHEN IWA_Task.IsExpired = 1 AND IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LEN(ExpiryActivatorUser.FullName) > 0 THEN ExpiryActivatorUser.FullName WHEN IWA_Task.IsExpired = 1 AND IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LEN(ExpiryActivatorUser.FullName) = 0 THEN ExpiryActivatorUser.Name WHEN IWA_Task.IsExpired = 1 AND IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LEN(ExpiryActivatorRole.DisplayNameTemplate) > 0 THEN ExpiryActivatorRole.DisplayNameTemplate WHEN IWA_Task.IsExpired = 1 AND IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LEN(ExpiryActivatorRole.DisplayNameTemplate) = 0 THEN ExpiryActivatorRole.Name WHEN IWA_Task.IsExpired = 0 AND IWA_Task.ActivatorUserId IS NOT NULL AND LEN(ActivatorUser.FullName) > 0 THEN ActivatorUser.FullName WHEN IWA_Task.IsExpired = 0 AND IWA_Task.ActivatorUserId IS NOT NULL AND LEN(ActivatorUser.FullName) = 0 THEN ActivatorUser.Name WHEN IWA_Task.IsExpired = 0 AND IWA_Task.ActivatorRoleId IS NOT NULL AND LEN(ActivatorRole.DisplayNameTemplate) > 0 THEN ActivatorRole.DisplayNameTemplate WHEN IWA_Task.IsExpired = 0 AND IWA_Task.ActivatorRoleId IS NOT NULL AND LEN(ActivatorRole.DisplayNameTemplate) = 0 THEN ActivatorRole.Name END,
    CASE WHEN IWA_Task.ActivatorRoleId IS NULL AND IWA_Task.ActivatorUserId IS NULL THEN 1 WHEN IWA_Task.ActivatorUserId IS NOT NULL AND ActivatorUser.UserState > 0 THEN 1 ELSE 0 END
  FROM IWA_Task
    INNER JOIN IWA_Case ON IWA_Task.CaseId = IWA_Case.CaseId
    LEFT OUTER JOIN IWA_User AS ActivatorUser ON IWA_Task.ActivatorUserId = ActivatorUser.UserId
    LEFT OUTER JOIN IWA_Role AS ActivatorRole ON IWA_Task.ActivatorRoleId = ActivatorRole.RoleId
    LEFT OUTER JOIN IWA_User AS ExpiryActivatorUser ON IWA_Task.ExpiryActivatorUserId = ExpiryActivatorUser.UserId
    LEFT OUTER JOIN IWA_Role AS ExpiryActivatorRole ON IWA_Task.ExpiryActivatorRoleId = ExpiryActivatorRole.RoleId
GO

