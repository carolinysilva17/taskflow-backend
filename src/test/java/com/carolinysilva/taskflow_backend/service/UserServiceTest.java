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
    void register_deveSalvarUsuarioComSenhaHasheada_quandoEmailNaoExiste() {
        userService = new UserService(userRepository, passwordEncoder);
        when(userRepository.findByEmail("carol@teste.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("minhaSenha123")).thenReturn("hash-gerado");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register("Carol", "carol@teste.com", "minhaSenha123");

        assertThat(result.getPasswordHash()).isEqualTo("hash-gerado");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_deveLancarExcecao_quandoEmailJaExiste() {
        userService = new UserService(userRepository, passwordEncoder);
        when(userRepository.findByEmail("carol@teste.com"))
                .thenReturn(Optional.of(new User("Carol", "carol@teste.com", "hash")));

        assertThatThrownBy(() -> userService.register("Carol", "carol@teste.com", "minhaSenha123"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void findByEmail_deveLancarExcecao_quandoUsuarioNaoExiste() {
        userService = new UserService(userRepository, passwordEncoder);
        when(userRepository.findByEmail("nao-existe@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("nao-existe@teste.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
