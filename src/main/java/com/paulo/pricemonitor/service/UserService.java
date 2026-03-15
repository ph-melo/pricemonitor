package com.paulo.pricemonitor.service;

import com.paulo.pricemonitor.entity.User;
import com.paulo.pricemonitor.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }
}
