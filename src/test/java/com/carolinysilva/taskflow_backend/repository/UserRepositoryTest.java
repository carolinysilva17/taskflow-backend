package com.carolinysilva.taskflow_backend.repository;

import com.carolinysilva.taskflow_backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
}
