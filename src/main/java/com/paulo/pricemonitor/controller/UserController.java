package com.paulo.pricemonitor.controller;

import com.paulo.pricemonitor.entity.User;
import com.paulo.pricemonitor.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

        public UserController(UserService userService) {
            this.userService = userService;
        }

        @PostMapping
        public User createUser(@RequestBody User user){
            return userService.createUser(user);
        }
}
