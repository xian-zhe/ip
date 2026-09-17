package oz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link CommandType} enum.
 */
public class CommandTypeTest {

    @Test
    public void valueOf_validConstants_returnsEnumConstant() {
        for (CommandType type : CommandType.values()) {
            assertNotNull(type);
            assertEquals(type, CommandType.valueOf(type.name()));
        }
    }
}
