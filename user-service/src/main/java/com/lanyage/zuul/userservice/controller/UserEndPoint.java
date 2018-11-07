package com.lanyage.zuul.userservice.controller;

import com.lanyage.zuul.userservice.bean.User;
import com.netflix.appinfo.EurekaInstanceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


@RestController
public class UserEndPoint  {

    @Autowired
    private EurekaInstanceConfig eurekaInstanceConfig;

    private static final Logger logger = LoggerFactory.getLogger(UserEndPoint.class);
    @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public List<User> users() {
        logger.info("/users, Instance Id:{}, host:{}",eurekaInstanceConfig.getInstanceId(), eurekaInstanceConfig.getHostName(false));
        return create();
    }

    @GetMapping(value = "/users/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public User user(@PathVariable("id") Integer id) {
        logger.info("/users/id, Instance Id:{}, host:{}",eurekaInstanceConfig.getInstanceId(), eurekaInstanceConfig.getHostName(false));
        return create().stream().filter(u -> u.getId() == id).findFirst().get();
    }

    private List<User> create() {
        List<User> users = new ArrayList<>();
        users.add(new User(1, "lanyage", "lanyage"));
        users.add(new User(2, "jerry", "jerry"));
        return users;
    }
}
