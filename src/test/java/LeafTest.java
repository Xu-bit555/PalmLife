import com.tencent.devops.leaf.plugin.annotation.EnableLeafServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnableLeafServer
public class LeafTest {

    public static void main(String[] args) {
        SpringApplication.run(LeafTest.class, args);
    }
}
