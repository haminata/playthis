package club.playthis.playthis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;


@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class
})
public class PlaythisApplication {

    public static void main(String[] args) throws IllegalAccessException, InstantiationException {

        DbModel.register(User.class);
        DbModel.register(Musicroom.class);
        DbModel.register(Song.class);

        DbModel.syncAll();

        SpringApplication.run(PlaythisApplication.class, args);
    }
}
