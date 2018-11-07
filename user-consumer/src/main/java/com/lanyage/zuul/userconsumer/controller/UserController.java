package com.lanyage.zuul.userconsumer.controller;

import com.lanyage.zuul.userconsumer.bean.User;
import com.lanyage.zuul.userconsumer.service.UserService;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @HystrixCommand(fallbackMethod = "userList")
    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<User> users() {
        return userService.users();
    }

    private List<User> userList() {
        List<User> users = new ArrayList<>();
        users.add(null);
        users.add(null);
        users.add(null);
        return users;
    }
    @HystrixCommand(fallbackMethod = "userDetail")
    @GetMapping(value = "/user/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public User user(@PathVariable("id") Integer id) {
        return userService.user(id);
    }

    private User userDetail(Integer id) {
        User user = new User(id, "NOT FOUND", "NOT FOUND");
        return user;
    }
}
