package com.p3s.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Created by parvendan on 08/07/16.
 */
@Controller
public class EmployeeController {

    private static int counter;

    @RequestMapping("/myapi/hello")
    @ResponseBody
    public String hello() {
        return "hello";
    }

}
