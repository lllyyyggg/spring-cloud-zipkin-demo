package com.lanyage.zuul.userconsumer.service;

import com.lanyage.zuul.userconsumer.bean.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@FeignClient(name = "USER-SERVICE")
public interface UserService {

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    List<User> users();

    @RequestMapping(value = "/users/{id}", method = RequestMethod.GET)
    User user(@PathVariable("id") Integer id);
}
