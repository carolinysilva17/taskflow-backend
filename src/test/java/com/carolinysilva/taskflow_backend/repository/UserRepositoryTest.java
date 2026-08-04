package com.carolinysilva.taskflow_backend.repository;

import com.carolinysilva.taskflow_backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_shouldReturnUser_whenEmailExists() {
        User user = new User("Carol", "carol.repo.test@test.com", "hash123");
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByEmail("carol.repo.test@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Carol");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenEmailDoesNotExist() {
        Optional<User> found = userRepository.findByEmail("does-not-exist@test.com");

        assertThat(found).isEmpty();
    }

    @Test
    void save_shouldThrowDataIntegrityViolation_whenEmailAlreadyExists() {
        userRepository.saveAndFlush(new User("Carol", "carol.repo.duplicate@test.com", "hash123"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("Outra Carol", "carol.repo.duplicate@test.com", "hash456")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
