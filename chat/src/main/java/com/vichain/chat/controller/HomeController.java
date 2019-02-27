package com.vichain.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @description:
 * @author: Mr.Joe
 * @create:
 */
@Controller
@RequestMapping
public class HomeController {
    @RequestMapping("/push")
    public String push(){
        return "/demo/push1";
    }
}
