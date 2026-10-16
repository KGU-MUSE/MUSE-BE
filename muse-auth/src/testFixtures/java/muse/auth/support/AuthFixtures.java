package muse.auth.support;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuthFixtures {
    private AuthFixtures() {}

    public static UserDetails user() {
        return User.withUsername("test-user")
                .password("{noop}test-password")
                .roles("USER")
                .build();
    }
}
