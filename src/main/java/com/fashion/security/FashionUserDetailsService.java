package com.fashion.security;

import com.fashion.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class FashionUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public FashionUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(username)
                .map(FashionUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
