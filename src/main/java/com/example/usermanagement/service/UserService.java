package com.example.usermanagement.service;

import com.example.usermanagement.dto.UserRequest;
import com.example.usermanagement.exception.UserNotFoundException;
import com.example.usermanagement.model.User;
import com.example.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    public User createUser(UserRequest request) {
        User user = new User(null, request.getFirstName(), request.getLastName(), request.getAge(), request.getGender());
        return userRepository.save(user);
    }

    public User updateUser(Long id, UserRequest request) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        User user = new User(id, request.getFirstName(), request.getLastName(), request.getAge(), request.getGender());
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }
}
