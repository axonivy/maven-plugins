-- ---------------------------------------------------------------------------------
-- 
-- This script was automatically generated. Do not edit it. Instead edit the source file
-- 
-- ---------------------------------------------------------------------------------
-- Database: Oracle
-- ---------------------------------------------------------------------------------
-- Copyright:
-- AXON IVY AG, Baarerstrasse 12, 6300 Zug
-- ---------------------------------------------------------------------------------

CREATE TABLE IWA_Task
(
  TaskId NUMBER(20) NOT NULL,
  ActivatorRoleId NUMBER(20),
  ActivatorUserId NUMBER(20),
  ExpiryActivatorRoleId NUMBER(20),
  ExpiryActivatorUserId NUMBER(20),
  IsExpired NUMBER(1) NOT NULL,
  PRIMARY KEY (TaskId)
)
TABLESPACE ${tablespace};

CREATE TABLE IWA_Case
(
  CaseId NUMBER(20) NOT NULL
)
TABLESPACE ${tablespace};

CREATE TABLE IWA_User
(
  UserId NUMBER(20) NOT NULL,
  Name VARCHAR2(200),
  FullName VARCHAR2(200) DEFAULT ''
)
TABLESPACE ${tablespace};

CREATE TABLE IWA_Role
(
  RoleId NUMBER(20) NOT NULL,
  Name VARCHAR2(200),
  DisplayNameTemplate VARCHAR2(200)
)
TABLESPACE ${tablespace};

ALTER TABLE IWA_Task ADD
(
 FOREIGN KEY (ActivatorRoleId) REFERENCES IWA_Role(RoleId) ON DELETE SET NULL
);

ALTER TABLE IWA_Task ADD
(
 FOREIGN KEY (ActivatorUserId) REFERENCES IWA_User(UserId) ON DELETE SET NULL
);

ALTER TABLE IWA_Task ADD
(
 FOREIGN KEY (ExpiryActivatorRoleId) REFERENCES IWA_Role(RoleId) ON DELETE SET NULL
);

ALTER TABLE IWA_Task ADD
(
 FOREIGN KEY (ExpiryActivatorUserId) REFERENCES IWA_User(UserId) ON DELETE SET NULL
);

CREATE VIEW IWA_TaskQuery
(
  TaskId,
  ActivatorDisplayName,
  ExpiryActivatorDisplayName,
  CurrentActivatorDisplayName
)
AS
  SELECT
    IWA_Task.TaskId,
    CASE WHEN IWA_Task.ActivatorUserId IS NOT NULL AND LENGTH(TRIM(ActivatorUser.FullName)) > 0 THEN ActivatorUser.FullName WHEN IWA_Task.ActivatorUserId IS NOT NULL AND LENGTH(TRIM(ActivatorUser.FullName)) = 0 THEN ActivatorUser.Name WHEN IWA_Task.ActivatorRoleId IS NOT NULL AND LENGTH(TRIM(ActivatorRole.DisplayNameTemplate)) > 0 THEN ActivatorRole.DisplayNameTemplate WHEN IWA_Task.ActivatorRoleId IS NOT NULL AND LENGTH(TRIM(ActivatorRole.DisplayNameTemplate)) = 0 THEN ActivatorRole.Name END,
    CASE WHEN IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LENGTH(TRIM(ExpiryActivatorUser.FullName)) > 0 THEN ExpiryActivatorUser.FullName WHEN IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LENGTH(TRIM(ExpiryActivatorUser.FullName)) = 0 THEN ExpiryActivatorUser.Name WHEN IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LENGTH(TRIM(ExpiryActivatorRole.DisplayNameTemplate)) > 0 THEN ExpiryActivatorRole.DisplayNameTemplate WHEN IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LENGTH(TRIM(ExpiryActivatorRole.DisplayNameTemplate)) = 0 THEN ExpiryActivatorRole.Name ELSE NULL END,
    CASE WHEN IWA_Task.IsExpired = 1 AND IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LENGTH(TRIM(ExpiryActivatorUser.FullName)) > 0 THEN ExpiryActivatorUser.FullName WHEN IWA_Task.IsExpired = 1 AND IWA_Task.ExpiryActivatorUserId IS NOT NULL AND LENGTH(TRIM(ExpiryActivatorUser.FullName)) = 0 THEN ExpiryActivatorUser.Name WHEN IWA_Task.IsExpired = 1 AND IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LENGTH(TRIM(ExpiryActivatorRole.DisplayNameTemplate)) > 0 THEN ExpiryActivatorRole.DisplayNameTemplate WHEN IWA_Task.IsExpired = 1 AND IWA_Task.ExpiryActivatorRoleId IS NOT NULL AND LENGTH(TRIM(ExpiryActivatorRole.DisplayNameTemplate)) = 0 THEN ExpiryActivatorRole.Name WHEN IWA_Task.IsExpired = 0 AND IWA_Task.ActivatorUserId IS NOT NULL AND LENGTH(TRIM(ActivatorUser.FullName)) > 0 THEN ActivatorUser.FullName WHEN IWA_Task.IsExpired = 0 AND IWA_Task.ActivatorUserId IS NOT NULL AND LENGTH(TRIM(ActivatorUser.FullName)) = 0 THEN ActivatorUser.Name WHEN IWA_Task.IsExpired = 0 AND IWA_Task.ActivatorRoleId IS NOT NULL AND LENGTH(TRIM(ActivatorRole.DisplayNameTemplate)) > 0 THEN ActivatorRole.DisplayNameTemplate WHEN IWA_Task.IsExpired = 0 AND IWA_Task.ActivatorRoleId IS NOT NULL AND LENGTH(TRIM(ActivatorRole.DisplayNameTemplate)) = 0 THEN ActivatorRole.Name END
  FROM IWA_Task
    INNER JOIN IWA_Case ON IWA_Task.CaseId = IWA_Case.CaseId
    LEFT OUTER JOIN IWA_User ActivatorUser ON IWA_Task.ActivatorUserId = ActivatorUser.UserId
    LEFT OUTER JOIN IWA_Role ActivatorRole ON IWA_Task.ActivatorRoleId = ActivatorRole.RoleId
    LEFT OUTER JOIN IWA_User ExpiryActivatorUser ON IWA_Task.ExpiryActivatorUserId = ExpiryActivatorUser.UserId
    LEFT OUTER JOIN IWA_Role ExpiryActivatorRole ON IWA_Task.ExpiryActivatorRoleId = ExpiryActivatorRole.RoleId;

