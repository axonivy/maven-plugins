ALTER TABLE ReferenceTable DROP
 CONSTRAINT ReferenceTable_DropTableId_fkey
;


-- Drop no longer existing table
DROP TABLE DropTable;

-- Dropped columns of table ReferenceTable
ALTER TABLE ReferenceTable DROP COLUMN DropTableId;