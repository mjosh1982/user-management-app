package com.example.usermanagement.service;

import com.example.usermanagement.dto.UserRequest;
import com.example.usermanagement.exception.ErrorCode;
import com.example.usermanagement.exception.UserAlreadyExistsException;
import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.model.Gender;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final UserRequest REQUEST = new UserRequest("Diana", "Prince", 28, Gender.FEMALE);

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Nested
    class GetAllUsers {

        @Test
        void returnsRepositoryResult() {
            List<User> users = List.of(new User(1L, "Alice", "Smith", 30, Gender.FEMALE));
            when(userRepository.findAll()).thenReturn(users);

            assertThat(userService.getAllUsers()).isEqualTo(users);
        }

        @Test
        void returnsEmptyListWhenNoUsers() {
            when(userRepository.findAll()).thenReturn(List.of());

            assertThat(userService.getAllUsers()).isEmpty();
        }
    }

    @Nested
    class GetUser {

        @Test
        void returnsUserWhenFound() {
            User user = new User(1L, "Alice", "Smith", 30, Gender.FEMALE);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThat(userService.getUser(1L)).isEqualTo(user);
        }

        @Test
        void throwsUserNotFoundWhenMissing() {
            when(userRepository.findById(42L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUser(42L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User with id 42 not found")
                    .satisfies(ex -> {
                        UserNotFoundException unf = (UserNotFoundException) ex;
                        assertThat(unf.getUserId()).isEqualTo(42L);
                        assertThat(unf.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                    });
        }
    }

    @Nested
    class CreateUser {

        @Test
        void savesUserWithoutIdAndReturnsSavedUser() {
            when(userRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Diana", "Prince"))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class)))
                    .thenAnswer(inv -> {
                        User u = inv.getArgument(0);
                        return new User(7L, u.getFirstName(), u.getLastName(), u.getAge(), u.getGender());
                    });

            User created = userService.createUser(REQUEST);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue()).isEqualTo(new User(null, "Diana", "Prince", 28, Gender.FEMALE));
            assertThat(created).isEqualTo(new User(7L, "Diana", "Prince", 28, Gender.FEMALE));
        }

        @Test
        void throwsConflictWhenNameAlreadyExists() {
            when(userRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Diana", "Prince"))
                    .thenReturn(Optional.of(new User(3L, "diana", "prince", 40, Gender.FEMALE)));

            assertThatThrownBy(() -> userService.createUser(REQUEST))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessage("User 'Diana Prince' already exists")
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateUser {

        @Test
        void replacesExistingUserKeepingId() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(userRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Diana", "Prince"))
                    .thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User updated = userService.updateUser(1L, REQUEST);

            assertThat(updated).isEqualTo(new User(1L, "Diana", "Prince", 28, Gender.FEMALE));
            verify(userRepository).save(updated);
        }

        @Test
        void allowsKeepingTheSameName() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(userRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Diana", "Prince"))
                    .thenReturn(Optional.of(new User(1L, "Diana", "Prince", 27, Gender.FEMALE)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThat(userService.updateUser(1L, REQUEST).getAge()).isEqualTo(28);
        }

        @Test
        void throwsConflictWhenAnotherUserHasTheName() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(userRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase("Diana", "Prince"))
                    .thenReturn(Optional.of(new User(2L, "Diana", "Prince", 40, Gender.FEMALE)));

            assertThatThrownBy(() -> userService.updateUser(1L, REQUEST))
                    .isInstanceOf(UserAlreadyExistsException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        void throwsUserNotFoundWhenMissing() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> userService.updateUser(99L, REQUEST))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User with id 99 not found");
            verify(userRepository, never()).save(any());
            verify(userRepository, never()).findByFirstNameIgnoreCaseAndLastNameIgnoreCase(any(), any());
        }
    }

    @Nested
    class DeleteUser {

        @Test
        void deletesExistingUser() {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.deleteUser(1L);

            verify(userRepository).deleteById(1L);
        }

        @Test
        void throwsUserNotFoundWhenMissing() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(99L))
                    .isInstanceOf(UserNotFoundException.class);
            verify(userRepository, never()).deleteById(anyLong());
        }
    }
}
