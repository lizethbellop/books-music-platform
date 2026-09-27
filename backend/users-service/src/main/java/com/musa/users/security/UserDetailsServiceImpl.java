package com.musa.users.security;

import com.musa.users.repository.UserRepository;
import lombok.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @NonNull
    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        com.musa.users.entity.User userEntity = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el correo: " + username));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(userEntity.getRole().getName());

        return new User(
                userEntity.getEmail(),
                userEntity.getPasswordHash(),
                userEntity.getIsActive(),
                true,
                true,
                true,
                Collections.singletonList(authority)
        );
    }
}
