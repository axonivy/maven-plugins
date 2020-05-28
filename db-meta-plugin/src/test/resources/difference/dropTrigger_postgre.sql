-- Drop trigger which depend on changed table(s)
DROP TRIGGER UserDeleteTrigger ON User;

-- Recreate trigger which depend on changed table(s)
DROP FUNCTION IF EXISTS UserDeleteTriggerFunc();

CREATE FUNCTION UserDeleteTriggerFunc() RETURNS TRIGGER AS '
  BEGIN
    UPDATE Test
    SET WorkerUserId=NULL
    WHERE "Test".WorkerUserId = OLD.UserId;

    RETURN OLD;
  END;
' LANGUAGE plpgsql;

CREATE TRIGGER UserDeleteTrigger AFTER DELETE
ON User FOR EACH ROW
EXECUTE PROCEDURE UserDeleteTriggerFunc();