package com.carolinysilva.taskflow_backend.service;

import com.carolinysilva.taskflow_backend.entity.User;
import com.carolinysilva.taskflow_backend.exception.BusinessRuleException;
import com.carolinysilva.taskflow_backend.exception.ResourceNotFoundException;
import com.carolinysilva.taskflow_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @Test
    void register_shouldSaveUserWithHashedPassword_whenEmailDoesNotExist() {
        userService = new UserService(userRepository, passwordEncoder);
        when(userRepository.findByEmail("carol@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("myPassword123")).thenReturn("generated-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register("Carol", "carol@test.com", "myPassword123");

        assertThat(result.getPasswordHash()).isEqualTo("generated-hash");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        userService = new UserService(userRepository, passwordEncoder);
        when(userRepository.findByEmail("carol@test.com"))
                .thenReturn(Optional.of(new User("Carol", "carol@test.com", "hash")));

        assertThatThrownBy(() -> userService.register("Carol", "carol@test.com", "myPassword123"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void findByEmail_shouldThrowException_whenUserDoesNotExist() {
        userService = new UserService(userRepository, passwordEncoder);
        when(userRepository.findByEmail("does-not-exist@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("does-not-exist@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void register_shouldNormalizeEmail_whenEmailHasSpaceOrUppercase() {
        userService = new UserService(userRepository, passwordEncoder);
        when(userRepository.findByEmail("carol@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("generated-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register("Carol", "  Carol@Test.com  ", "myPassword123");

        assertThat(result.getEmail()).isEqualTo("carol@test.com");
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExistsWithDifferentCasing() {
        userService = new UserService(userRepository, passwordEncoder);
        when(userRepository.findByEmail("carol@test.com"))
                .thenReturn(Optional.of(new User("Carol", "carol@test.com", "hash")));

        assertThatThrownBy(() -> userService.register("Carol", "CAROL@TEST.COM", "myPassword123"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
