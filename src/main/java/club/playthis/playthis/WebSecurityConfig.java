package club.playthis.playthis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/",
                        "/ws",
                        "/components.js",
                        "/app.js",
                        "/schemas",
                        "/spotify_token",
                        "/dbmodel.js",
                        "/db/musicrooms",
                        "/db/users",
                        "/db/songs",
                        "/lib.js",
                        "/home").permitAll()
                .anyRequest()
                .authenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
                .logout()
                .permitAll();
    }

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {

            @Override
            public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
                club.playthis.playthis.User user = club.playthis.playthis.User.findOne(club.playthis.playthis.User.class, new DbModel.Where() {{
                    put(club.playthis.playthis.User.ATTR_NAME, s);
                }});
                System.out.println("[loadUserByUsername] username: " + s + ", user" + user);
                if (user == null) {
                    return User.builder()
                            .authorities(new GrantedAuthority() {
                                @Override
                                public String getAuthority() {
                                    return null;
                                }
                            })
                            .build();
                }

                return User.builder()
                        .username(s)
                        .password((String) user.getValue(club.playthis.playthis.User.ATTR_PASSWORD_HASH))
                        .disabled(user.deletedAt != null)
                        .authorities(new GrantedAuthority() {
                            @Override
                            public String getAuthority() {
                                return user.getId().toString();
                            }
                        })
                        .build();
            }

        };
    }
}
