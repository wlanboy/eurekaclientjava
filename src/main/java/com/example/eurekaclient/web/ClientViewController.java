package com.example.eurekaclient.web;

import com.example.eurekaclient.services.LifecycleManager;
import com.example.eurekaclient.services.ServiceInstance;
import com.example.eurekaclient.services.ServiceInstanceStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ClientViewController {

    private final ServiceInstanceStore store;
    private final LifecycleManager lifecycleManager;

    @GetMapping("/")
    public String defaultView(Model model) {
        return renderClientList(model);
    }

    @GetMapping("/clients")
    public String listClients(Model model) {
        return renderClientList(model);
    }

    private String renderClientList(Model model) {
        List<ServiceInstance> dbClients = store.getInstances();
        List<ServiceInstance> runningClients = lifecycleManager.getRunningInstances();

        model.addAttribute("dbClients", dbClients);
        model.addAttribute("runningClients", runningClients);

        log.debug("Rendering clients view: {} DB clients, {} running clients",
                dbClients.size(), runningClients.size());

        return "clients";
    }
}
