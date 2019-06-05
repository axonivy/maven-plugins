ALTER TABLE ReferenceTable DROP
 CONSTRAINT ReferenceTable_DropTableId_fkey
;


-- Drop no longer exisiting table
DROP TABLE DropTable;

-- Dropped columns of table ReferenceTable
ALTER TABLE ReferenceTable DROP COLUMN DropTableId;