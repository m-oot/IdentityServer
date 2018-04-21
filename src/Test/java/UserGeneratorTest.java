import org.junit.Test;
import Identity.Generator.UserGenerator;

public class UserGeneratorTest {

    @Test
    public void testRandomName() {
        UserGenerator.Initialize();
        for(int i = 0; i < 100; i++) {
            UserGenerator.randomName();
        }
    }
}
