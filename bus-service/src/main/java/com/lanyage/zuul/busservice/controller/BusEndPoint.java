package com.lanyage.zuul.busservice.controller;

import com.lanyage.zuul.busservice.bean.Bus;
import com.netflix.appinfo.EurekaInstanceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class BusEndPoint {


    @Autowired
    private EurekaInstanceConfig eurekaInstanceConfig;
    private static final Logger logger = LoggerFactory.getLogger(BusEndPoint.class);

    @RequestMapping(value = "/buses", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    List<Bus> buses() {
        logger.info("/buses, Instance Id:{}, host:{}", eurekaInstanceConfig.getInstanceId(), eurekaInstanceConfig.getHostName(false));
        return create();
    }

    @GetMapping(value = "/buses/{version}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Bus bus(@PathVariable("version") String version) {
        logger.info("/buses/id, Instance Id:{}, host:{}", eurekaInstanceConfig.getInstanceId(), eurekaInstanceConfig.getHostName(false));
        return create().stream().filter(u -> u.getVersion().equals(version)).findFirst().get();
    }

    private List<Bus> create() {
        List<Bus> buses = new ArrayList<>();
        buses.add(new Bus("BENZI", "GLK"));
        buses.add(new Bus("BMW", "X4"));
        return buses;
    }
}
